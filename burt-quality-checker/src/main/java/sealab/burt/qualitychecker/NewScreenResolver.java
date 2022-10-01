package sealab.burt.qualitychecker;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.linear.IllConditionedOperatorException;
import org.javatuples.Triplet;
import sealab.burt.qualitychecker.actionmatcher.ActionMatchingException;
import sealab.burt.qualitychecker.actionmatcher.NLActionS2RMatcher;
import sealab.burt.qualitychecker.graph.*;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import sealab.burt.qualitychecker.similarity.EmbeddingSimilarityComputer;
public @Slf4j
class NewScreenResolver {

    private final int graphMaxDepthCheck;

    public NewScreenResolver(int graphMaxDepthCheck) {
        this.graphMaxDepthCheck = graphMaxDepthCheck;
    }

    private static void getCandidateGraphStates(AppGraph<GraphState, GraphTransition> executionGraph,
                                                LinkedHashMap<GraphState, Integer> stateCandidates,
                                                GraphState currentState,
                                                Integer currentDistance,
                                                int maxDistanceToCheck) {
        if (currentState == null) {
            return;
        }
        if (currentDistance > maxDistanceToCheck) {
            return;
        }

        if (stateCandidates.containsKey(currentState) || GraphState.END_STATE.equals(currentState))
            return;

        // If the node is not in the map then we add the state and the
        // distance from the current state on the graph
        stateCandidates.put(currentState, currentDistance);

//        if (executionGraph.containsVertex(currentState)) {
        Set<GraphTransition> outgoingEdges = executionGraph.outgoingEdgesOf(currentState);
        final Set<GraphState> nextStates = outgoingEdges.stream().map(GraphTransition::getTargetState)
                .collect(Collectors.toCollection(HashSet::new));
        nextStates.remove(currentState); // QUESTION: why do we remove this state?

        for (GraphState state : nextStates) {
            getCandidateGraphStates(executionGraph, stateCandidates, state, currentDistance + 1, maxDistanceToCheck);
        }
//        }
    }


    public List<Triplet<GraphState, String, Double>> resolveStateInAugmentedGraph(List<String> obDescriptions,
                                                                                AppGraphInfo executionGraph,
                                                                                GraphState currentState) throws Exception {

        // 1. Get all considered nodes that are in range of GRAPH_MAX_DEPTH_CHECK
        LinkedHashMap<GraphState, Integer> stateCandidates = new LinkedHashMap<>();
        getCandidateGraphStates(executionGraph.getGraph(), stateCandidates, currentState, 0, graphMaxDepthCheck);
        stateCandidates.remove(GraphState.START_STATE);

        log.debug("State candidates (" + stateCandidates.size() + "): " + stateCandidates);


        int nThreads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        List<Triplet<GraphState, String, Double>> matchedStates = new ArrayList<>();

        //list of all futures

        List<CompletableFuture<Triplet<GraphState, String, Double>>> futures = new ArrayList<>();
        for (Map.Entry<GraphState, Integer> candidateEntry : stateCandidates.entrySet()) {
            futures.add(CompletableFuture.supplyAsync(() ->
            {
                try {
                    return processCandidateState(obDescriptions, candidateEntry);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CompletionException(e);
                }

            }, executor));
        }

        log.debug("Waiting for futures: " + futures.size());

        // wait until all futures finish, and then continue with the processing
        try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

                //--------------------------------------------

                //aggregate results
                for (CompletableFuture<Triplet<GraphState, String, Double>> future : futures) {
                    Triplet<GraphState, String, Double> match = future.get();
                    if (match != null) {
                        matchedStates.add(match);
                    }
                }

            }
        catch(Exception e){
            e.printStackTrace();
            log.debug(e.getMessage());
        }
        finally{
            executor.shutdown();
        }

        // rank the steps
        matchedStates.sort((a, b) -> b.getValue2().compareTo(a.getValue2()));
        if( matchedStates.size() > 5){
            log.debug("matchedStates" + matchedStates.subList(0, 4));
            return matchedStates.stream().limit(5).collect(Collectors.toList());
        }
        log.debug("matchedStates" + matchedStates);

        return matchedStates;

    }


    private List<ImmutablePair<GraphState, Double>> rankMatchedStates(List<ImmutablePair<GraphState, Double>> matchedStates) {
        log.debug("Matched states (" + matchedStates.size() + "):" + matchedStates);


        //---------------------------------------------

        // sort based on the score
//        List<ImmutablePair<GraphState, Double>> stateScores = new ArrayList<>();

        // Give priority to components based on how far they are from the current
        // state, so the closer they are the higher the score
//        for (Map.Entry<GraphState, Double> stateEntry : matchedStates.entrySet()) {
//            final GraphState step = stateEntry.getKey();
//            final Double distance = stateEntry.getValue();
//
//            double score = 1d / (distance + 1);
//            stateScores.add(new ImmutablePair<>(step, score));
//        }
        matchedStates.sort((a, b) -> b.right.compareTo(a.right));

        return matchedStates;
    }

    // new code
    private Triplet<GraphState, String, Double> processCandidateState(List<String> ObDescriptions,
                                                                    Map.Entry<GraphState, Integer> candidateEntry) throws Exception {
        final GraphState candidateState = candidateEntry.getKey();
        final Integer distance = candidateEntry.getValue();


        //-------------------------------------
        // Get the components of the current candidate screen

        List<AppGuiComponent> stateComponents = candidateState.getComponents();
        if (stateComponents == null)
            return null;

//        for (AppGuiComponent stateComponent : stateComponents){
//            log.debug("Checking phrases: " + stateComponent.getPhrases().toString());
//        }

        //filter out those components with phrases
        stateComponents = stateComponents.stream()
                .filter(c -> c.getPhrases() != null && !c.getPhrases().isEmpty())
                .collect(Collectors.toList());


        if (stateComponents.isEmpty())
            return null;

        List<String> phrases = new ArrayList<>();

        stateComponents.forEach(e -> phrases.addAll(e.getPhrases()));

        if (phrases.isEmpty()){
            return null;
        }

        //-------------------------------------

        // try to match one state

        return determineComponentForOb(ObDescriptions,
                phrases, candidateState);

    }


    public  Triplet<GraphState, String, Double> determineComponentForOb(List<String> ObDescriptions, List<String> phrases, GraphState candidateState)
            throws Exception {

        List<Triplet<GraphState, String, Double>> matchedStatesForEachCandidateState = new ArrayList<>();

        for (String ObDescription : ObDescriptions) {
            List<Double> scores = EmbeddingSimilarityComputer.computeSimilarities(ObDescription, phrases);
            if (Collections.max(scores) > 0.35) {
                log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>" + "\n" +
                        "Checking candidate state/screen: " + candidateState.getUniqueHash() + "\n" +
                        "Checking candidate phrases " + phrases.toString() + "\n" +
                        "Checking matched scores " + scores.toString());

                matchedStatesForEachCandidateState.add(new Triplet<>(candidateState, ObDescription, Collections.max(scores)));
            }
        }
        matchedStatesForEachCandidateState.sort((a, b) -> b.getValue2().compareTo(a.getValue2()));
        if ( !matchedStatesForEachCandidateState.isEmpty()) {
            return matchedStatesForEachCandidateState.get(0);

        }
        return null;

    }




    private Map.Entry<AppGuiComponent, Double> findComponent(String ObDescription,
                                                                 List<AppGuiComponent> currentScreen){

        for (AppGuiComponent guiComponent: currentScreen){
            List<String> phrases = guiComponent.getPhrases();
            // match ObDescription with phrases
        }

        return null;
    }





}
