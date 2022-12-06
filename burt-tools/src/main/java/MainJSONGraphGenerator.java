import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import com.opencsv.CSVWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.javatuples.Triplet;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import sealab.burt.BurtConfigPaths;
import sealab.burt.qualitychecker.JSONGraphReader;
import sealab.burt.qualitychecker.actionmatcher.GraphLayout;
import sealab.burt.qualitychecker.actionmatcher.GraphUtils;
import sealab.burt.qualitychecker.graph.*;
import seers.appcore.csv.CSVHelper;
import seers.appcore.utils.JavaUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;


public
@Slf4j
class MainJSONGraphGenerator {

    private static final Set<Triplet<String, String, String>> ALL_SYSTEMS = JavaUtils.getSet(
            new Triplet<>("2", "familyfinance", "1.5.5-DEBUG")
            // , 
            // new Triplet<>("8", "trickytripper", "1.6.0"),
            // new Triplet<>("10","files", "1.0.0-beta.11")
            
            // , new Triplet<>("18","calendula", "2.5.7"),
            // new Triplet<>("19","streetcomplete", "5.2"), new Triplet<>("21","atimetracker", "0.51.1"),
            // new Triplet<>("44","omninotes", "5.5.2"), new Triplet<>("53","markor", "2.3.1"),
            // new Triplet<>("71","kiss", "3.13.5"), new Triplet<>("117","openfoodfacts", "2.9.8"),
            // new Triplet<>("128","andotp", "0.7.1.1-dev"), new Triplet<>("129","andotp", "0.7.0-dev"),
            // new Triplet<>("130","andotp", "0.6.3.1-dev"), 
            // new Triplet<>("135","commons", "2.9.0-debug"),
            // new Triplet<>("191","anuto", "0.2-1"), new Triplet<>("201","inaturalist", "1.5.1"),
            // new Triplet<>("206","gnucash", "2.1.3"), new Triplet<>("209","gnucash", "2.2.0"),
            // new Triplet<>("256","gnucash", "2.1.4"), new Triplet<>("1066","focus", "7.0"),
            // new Triplet<>("1067","focus", "7.0"), new Triplet<>("1073","focus", "5.2"),
            // new Triplet<>("1096","inaturalist", "1.13.9"), new Triplet<>("1145","gpstest", "3.8.1"),
            // new Triplet<>("1146","gpstest", "3.8.0"), new Triplet<>("1147","gpstest", "3.0.0"),
            // new Triplet<>("1149","gpstest", "3.2.11"), new Triplet<>("1151","gpstest", "3.0.1"),
            // new Triplet<>("1152","gpstest", "3.0.2"), new Triplet<>("1202","createpdf", "6.6.0"),
            // new Triplet<>("1205","createpdf", "8.5.7"), new Triplet<>("1207","andotp", "0.4.0.1"),
            // new Triplet<>("1214","andotp", "0.7.1.1"), new Triplet<>("1215","andotp", "0.7.1.1"),
            // new Triplet<>("1223","gnucash", "2.2.0"), new Triplet<>("1224","gnucash", "2.1.3"),
            // new Triplet<>("1226","gnucash", "2.1.4"), new Triplet<>("1299","fieldbook", "4.3.3"),
            // new Triplet<>("1399","phimpme", "1.4.0"), 
            // new Triplet<>("1406","phimpme", "1.4.0"),
            // new Triplet<>("1430","fastnfitness", "0.19.0.1"), new Triplet<>("1441","anglerslog", "1.2.5"),
            // new Triplet<>("1445","anglerslog", "1.3.1"), new Triplet<>("1481","hex", "0.1.0"),
            // new Triplet<>("1645","trainerapp", "1.0")
    );

    private static final String outFolder = Path.of("..", "data", "graphs_json_data").toString();
    private static final Logger log = LoggerFactory.getLogger(MainJSONGraphGenerator.class);

    public static void main(String[] args) throws Exception {

        int nThreads = 1;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        //list of all futures
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        try {
            for (Triplet<String, String, String> system : ALL_SYSTEMS) {
                futures.add(CompletableFuture.supplyAsync(new Supplier<>() {
                    @SneakyThrows
                    @Override
                    public Boolean get() {
                        try {
                            generateAndSaveGraph(system);
                        } catch (Exception e) {
                           log.error("Unexpected error", e);
                        }
                        return true;
                    }
                }, executor));
            }


            log.debug("Waiting for futures: " + futures.size());

            //wait until all futures finish, and then continue with the processing
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenAccept(ignored -> log.debug("All systems were processed")
                    ).exceptionally(exception -> {
                        log.error("There was an error: " + exception.getMessage(), exception);
                        return null;
                    }).join();

        } finally {
            executor.shutdown();
        }

        log.debug("Reached end");

    }

    private static void generateAndSaveGraph(Triplet<String, String, String> system) throws Exception {
        log.debug("Processing system: " + system);

        //AppGraphInfo graphInfo = JSONGraphReader.getGraph(system.getLeft(), system.getRight(), bugID);

        AppGraphInfo graphInfo = JSONGraphReader.getGraph(system.getValue1(), system.getValue2(),system.getValue0(), 
                                GraphDataSource.BOTH);

        AppGraph<GraphState, GraphTransition> graph = graphInfo.getGraph();
        Appl app = graphInfo.getApp();

        //----------------------------------------

        String sysString = File.separator + app.getId() + "-" + app.getPackageName() + "-" + app.getVersion();

        File sysFolder = new File(outFolder + File.separator + "Bug" + system.getValue0() + File.separator +sysString);

        if (sysFolder.exists()) {
            FileUtils.deleteDirectory(sysFolder);
        }

        String pathname = sysFolder + File.separator + sysString;

        // ------------------------------------------------------

        File graphFile = new File(pathname + "-graph.txt");
        String graphStr = graphInfo.graphToString();
        FileUtils.write(graphFile, graphStr, StandardCharsets.UTF_8);

        // ------------------------------------------------------

        KosarajuStrongConnectivityInspector<GraphState, GraphTransition> inspector =
                new KosarajuStrongConnectivityInspector<>(graph);
        List<Set<GraphState>> stronglyConnectedSets = inspector.stronglyConnectedSets();

        String ccStr = getConnectedComponentsStr(stronglyConnectedSets);
        File ccFile = new File(pathname + "-connected-comp.txt");
        FileUtils.write(ccFile, ccStr, StandardCharsets.UTF_8);

        // ------------------------------------------------------

        String packageName = app.getPackageName();

        String pathnameStates = sysFolder + File.separator + "states";
        String pathnameTransitions = sysFolder + File.separator + "transitions";

        new File(pathnameStates).mkdir();
        new File(pathnameTransitions).mkdir();

        Set<GraphTransition> edgeSet = graph.edgeSet();

        List<List<String>> nodeSources = new ArrayList<>();

        for (GraphTransition edge : edgeSet) {

            String screenshotFile = edge.getStep().getScreenshotFile();

            if (screenshotFile == null) {
//                log.error("Step has no screenshot: " + edge.getId());
                continue;
            }

//            String dataLocation =
//                    Paths.get(BurtConfigPaths.crashScopeDataPath, String.join("-", packageName, app.getVersion())).toString();

            String dataLocation =
                    Paths.get(BurtConfigPaths.crashScopeDataPath + "/CS" +system.getValue0(), String.join("-", packageName, app.getVersion())).toString();

            if (edge.getDataSource().equals(GraphDataSource.TR))
                dataLocation =
                        Paths.get(BurtConfigPaths.traceReplayerDataPath + "/TR" + system.getValue0()).toString();

            File srcFileStep = Path.of(dataLocation, "screenshots", screenshotFile).toFile();
            File srcFileState =
                    Path.of(dataLocation, "screenshots", edge.getSourceState().getScreenshotPath()).toFile();

            if (screenshotFile.endsWith(".png") && srcFileStep.exists() && srcFileStep.isFile()) {
                File destFile = new File(pathnameTransitions + File.separator + edge.getUniqueHash() + ".png");
                FileUtils.copyFile(srcFileStep, destFile);
            }

            if (srcFileState.exists()) {
                GraphState sourceState = edge.getSourceState();
                File destFile2 = new File(
                        pathnameStates + File.separator + sourceState.getUniqueHash() + ".png");
                if (!destFile2.exists()) {
                    FileUtils.copyFile(srcFileState, destFile2);

                    String screenId = sourceState.getUniqueHash();
                    Integer sequenceId = edge.getStep().getSequence();
                    String source = edge.getDataSource().toString();
                    Long executionId  = edge.getStep().getExecution();
                    String statePath = srcFileState.getPath();
                    String executionPath  = sourceState.getExecutionPath().toString();
                    String xmlPath = sourceState.getXmlPath().toString();

                    nodeSources.add(Arrays.asList(screenId.toString(), sequenceId.toString(), source, executionId.toString(), statePath, executionPath, xmlPath));
                } 

            }

        }

        //------------------

       //Function fn = null;
        //writeCsv(String filePath, List<String> header, List<T> data, List<String> entryPrefix,
       // Function<T, List<String>> entryFunction, char separator)
        CSVHelper.writeCsv(Paths.get(pathnameStates, "states.csv").toString(), 
        Arrays.asList("screen_id", "sequence_id", "source", "execution_id", 
                    "original_screenshot_path", "execution_path", "xml_path"), 
        nodeSources, null, Function.identity(), ',');

        // ------------------------------------------------------
        log.debug("Saving image");

        mxGraph visualGraph = GraphUtils.getVisualGraph(graph, GraphLayout.CIRCLE);
        BufferedImage image = mxCellRenderer.createBufferedImage(visualGraph, null, 1, Color.WHITE, true,
                null);
        ImageIO.write(image, "PNG", new File(pathname + ".png"));

        log.debug("Done");
    }

    private static String getConnectedComponentsStr(List<Set<GraphState>> stronglyConnectedSets) {

        StringBuilder builder = new StringBuilder();

        builder.append("# of components: " + stronglyConnectedSets.size());
        builder.append("\n");
        builder.append("\n");

        for (int i = 0; i < stronglyConnectedSets.size(); i++) {
            Set<GraphState> set = stronglyConnectedSets.get(i);

            builder.append("Component " + (i + 1));
            builder.append("\n");

            set.forEach(s -> {
                builder.append(s.getName() + ": " + s.getUnformattedXml());
                builder.append("\n");
            });

            builder.append("\n");
        }

        return builder.toString();
    }

}
