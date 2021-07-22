package sealab.burt.server.actions.s2r;

import lombok.extern.slf4j.Slf4j;
import org.jgrapht.GraphPath;
import sealab.burt.qualitychecker.S2RChecker;
import sealab.burt.qualitychecker.graph.AppStep;
import sealab.burt.qualitychecker.graph.GraphState;
import sealab.burt.qualitychecker.graph.GraphTransition;
import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.*;
import sealab.burt.server.msgparsing.Intent;
import sealab.burt.server.output.BugReportElement;

import java.util.*;
import java.util.stream.Collectors;

import static sealab.burt.server.StateVariable.*;

@Slf4j
public class ProvideFirstPredictedS2RAction extends ChatBotAction {

    public final static int MAX_NUMBER_OF_PATHS_TO_PROVIDE = 3;

    public ProvideFirstPredictedS2RAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

//    public static List<KeyValues> getPredictedStepOptions(S2RChecker s2rchecker, GraphPath<GraphState,
//            GraphTransition> path, ConversationState state, GraphState currentState) {
//        List<AppStep> pathWithLoops = getPathWithLoops(s2rchecker, path, state, currentState);
//
//        return SelectMissingS2RAction.getStepOptions(pathWithLoops, state);
//
//    }

    public static List<KeyValues> getPredictedStepOptionsFromAppSteps(List<AppStep> path, ConversationState state) {
        return SelectMissingS2RAction.getStepOptions(path, state);
    }

    public static List<AppStep> getPathWithLoops(S2RChecker s2rchecker, GraphPath<GraphState, GraphTransition> path,
                                                 ConversationState state, GraphState currentState) {
        // we convert the transitions to the steps
        List<AppStep> steps = convertGraphTransitionsToAppStep(path);

        List<BugReportElement> bugReportElements = (List<BugReportElement>) state.get(REPORT_S2R);

        // Add the state loops in order to the path
        AppStep lastStep = (AppStep) bugReportElements.get(bugReportElements.size() - 1).getOriginalElement();
        List<AppStep> currentResolvedSteps = new LinkedList<>();
        s2rchecker.executeIntermediateStepsInShortestPath(null, lastStep,
                currentResolvedSteps, steps, currentState.getComponents());

        //Get screenshots from the AppSteps
        //Show the first 5 steps of the path to the user

        List<AppStep> pathWithLoops = currentResolvedSteps.subList(0, Math.min(5, currentResolvedSteps.size()));
        log.debug(String.valueOf(pathWithLoops.size()));

        //setting the id, for testing purposes
        for (int i = 0; i < pathWithLoops.size(); i++) {
            AppStep step = pathWithLoops.get(i);
            step.setId((long) i);
        }
        return pathWithLoops;
    }

    public static List<AppStep> convertGraphTransitionsToAppStep(GraphPath<GraphState, GraphTransition> path) {
        // get app steps
        return path.getEdgeList().stream()
                .map(GraphTransition::getStep)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatBotMessage> execute(ConversationState state) {

        state.put(PREDICTING_S2R, true);

        //target state
        List<BugReportElement> obReportElements = (List<BugReportElement>) state.get(REPORT_OB);
        if (obReportElements == null) {
            return getNextStepMessage();
        }

        BugReportElement obReportElement = obReportElements.get(0);

        GraphState targetState;
        if (obReportElement == null || obReportElement.getOriginalElement() == null) {
            return getNextStepMessage();
        }

        targetState = (GraphState) obReportElement.getOriginalElement();

        //-----------------------------------------------

        //current state
        S2RChecker checker = (S2RChecker) state.get(S2R_CHECKER);
        GraphState currentState = checker.getCurrentState();

        //FIXME:check target state equals to current state

        //get all the paths according to the score
        List<GraphPath<GraphState, GraphTransition>> predictedPaths = checker.getAllPaths(targetState);
//        List<GraphPath<GraphState, GraphTransition>> predictedPaths = checker.getFirstKDummyPaths(
//                MAX_NUMBER_OF_PATHS, targetState);

        log.debug("Total number of predicted paths: " + predictedPaths.size());


        if (predictedPaths.isEmpty()) {
            return getNextStepMessage();
        }

        //----------------------------------------

        state.put(PREDICTED_S2R_PATHS, predictedPaths);
        state.put(PREDICTED_S2R_CURRENT_PATH, 0);


        // the method getPathWithLoops() only returns 5 steps
        List<List<AppStep>> pathsWithLoops = predictedPaths.stream()
                .map(path -> getPathWithLoops(checker, path, state, currentState))
                .collect(Collectors.toList());
        log.debug(String.valueOf(pathsWithLoops.get(0).size()));
        List<List<AppStep>> pathsWithLoopsRemoveDuplicated = new ArrayList<>(new LinkedHashSet<>(pathsWithLoops));
        state.put(StateVariable.PREDICTED_S2R_PATHS_WITH_LOOPS, pathsWithLoopsRemoveDuplicated);
        state.put(PREDICTED_S2R_NUMBER_OF_PATHS, Math.min(MAX_NUMBER_OF_PATHS_TO_PROVIDE,
                pathsWithLoopsRemoveDuplicated.size()));

        // get the first predicted path
        List<KeyValues> stepOptions = getPredictedStepOptionsFromAppSteps(
                pathsWithLoopsRemoveDuplicated.get((int) state.get(PREDICTED_S2R_CURRENT_PATH)), state); // it is 0,
        // first step
        if (stepOptions.isEmpty()) {
            return getNextStepMessage();
        }

        log.debug("Suggesting path #" + state.get(PREDICTED_S2R_CURRENT_PATH));

        setNextExpectedIntents(Collections.singletonList(Intent.S2R_PREDICTED_SELECTED));

        MessageObj messageObj = new MessageObj("Please click the “done” button when you are done.",
                WidgetName.S2RScreenSelector);
        return createChatBotMessages(
                "Okay, it seems the next steps that you performed might be the following.",
                "Can you confirm which ones you actually performed next?",
                "Remember that the screenshots below are for reference only.",
                new ChatBotMessage(messageObj, stepOptions, true));
    }

    private List<ChatBotMessage> getNextStepMessage() {
        setNextExpectedIntents(Collections.singletonList(Intent.S2R_DESCRIPTION));
        return createChatBotMessages("Okay, can you please provide the next step?");
    }

}
