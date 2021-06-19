package sealab.burt.qualitychecker;

import com.android.uiautomator.tree.BasicTreeNode;
import com.android.uiautomator.tree.UiHierarchyXmlLoader;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.semeru.android.core.entity.model.App;
import edu.semeru.android.core.entity.model.fusion.DynGuiComponent;
import edu.semeru.android.core.entity.model.fusion.Execution;
import edu.semeru.android.core.entity.model.fusion.Screen;
import edu.semeru.android.core.entity.model.fusion.Step;
import edu.semeru.android.core.model.DynGuiComponentVO;
import edu.semeru.android.testing.helpers.UiAutoConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import sealab.burt.nlparser.euler.actions.utils.AppNamesMappings;
import sealab.burt.qualitychecker.graph.AppGraphInfo;
import sealab.burt.qualitychecker.graph.db.GraphGenerator;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public @Slf4j
class JSONGraphReader {

    private static final ConcurrentHashMap<String, AppGraphInfo> graphs = new ConcurrentHashMap<>();

    public static AppGraphInfo getGraph(String baseFolder, String appName, String appVersion) throws Exception {
        AppGraphInfo graph = graphs.get(getKey(appName, appVersion));
        if (graph == null) {
            readGraph(baseFolder, appName, appVersion);
            graph = graphs.get(getKey(appName, appVersion));
        }
        return graph;
    }

    private static String getKey(String app, String appVersion) {
        return MessageFormat.format("{0}-{1}", app, appVersion);
    }

    public static String getFirstPackageName(String appName) {
        String normalizedAppName = AppNamesMappings.normalizeAppName(appName);

        if (normalizedAppName == null) throw new RuntimeException("Could not normalize app name: " + appName);

        List<String> packages = AppNamesMappings.getPackageNames(normalizedAppName.toLowerCase());
        if (packages == null || packages.isEmpty()) {
            throw new RuntimeException("No packages found for: " + appName);
        }

        return packages.get(0);
    }

    public static void readGraph(String baseFolder, String appName, String appVersion) throws Exception {

        String packageName = getFirstPackageName(appName);
        String dataLocation = Paths.get(baseFolder, String.join("-", packageName, appVersion)).toString();

        String key = getKey(appName, appVersion);
        log.debug("Reading graph from JSON files for " + key);

        //--------------------------

        List<Path> executionFiles = Files.find(Paths.get(dataLocation), 1,
                (path, attr) -> path.toFile().getName().startsWith("Execution-"))
                .collect(Collectors.toList());


        //----------------------
        List<Execution> executions = new ArrayList<>();
        Long componentId = 0L;
        App app = null;

        Gson gson = new Gson();
        for (int i = 0; i < executionFiles.size(); i++) {
            Path executionFile = executionFiles.get(i);


            //Set up a new Gson object
            JsonReader reader = new JsonReader(new FileReader(executionFile.toFile()));

            // De-serialize the Execution object.
            Execution execution = gson.fromJson(reader, Execution.class);
            execution.setId((long) i);

            app = execution.getApp();
            app.setId(1L);

            //----------------------------------------

            //Add IDs to steps
            List<Step> steps = execution.getSteps();
            for (int j = 0; j < execution.getSteps().size(); j++) {
                steps.get(j).setId((long) j);
            }
            execution.setSteps(steps);

            //----------------------------------------

            //This for loop reads in the list of DynGuiComponentVOs for each screen
            for (Step currStep : execution.getSteps()) {

                // Get the current screen and read in the corresponding xml file.
                // Note that I decrement the "sequenceStep" by one since it is not zero indexed.
                String xmlPath = Path.of(dataLocation, String.join("-",
                        execution.getApp().getPackageName(),
                        execution.getApp().getVersion(), String.valueOf(execution.getExecutionNum()),
                        execution.getExecutionType() + (currStep.getSequenceStep() - 1)) + ".xml")
                        .toString();

                //Parse the xml file into a BasicTreeNode and then set up variables required by the visitNodes method.
                UiHierarchyXmlLoader loader = new UiHierarchyXmlLoader();
                BasicTreeNode tree = loader.parseXml(xmlPath);
                StringBuilder builder = new StringBuilder();
                ArrayList<DynGuiComponentVO> currComps = new ArrayList<>();

                //Visit the nodes. The list of components should be returned in the "currComps" ArrayList as
                // DynGUIComponentVOs.
                Screen screen = currStep.getScreen();
                UiAutoConnector.visitNodes(screen.getActivity(), tree, currComps, 1080, 1920, true, null,
                        true, 0, builder, 1);

                List<DynGuiComponent> guiComponents = convertVOstoGUIComps(currComps, screen, componentId);
                screen.setDynGuiComponents(guiComponents);

//            System.out.println(guiComponents);
            }
            executions.add(execution);
        }

        //----------------------------------------

        GraphGenerator generator = new GraphGenerator();

        AppGraphInfo graphInfo = generator.generateGraph(executions, app);
        graphs.put(key, graphInfo);
    }

    private static List<DynGuiComponent> convertVOstoGUIComps(ArrayList<DynGuiComponentVO> currComps, Screen screen,
                                                              Long componentId) {

        HashMap<Long, Pair<DynGuiComponentVO, DynGuiComponent>> cache = new HashMap<>();

        for (DynGuiComponentVO voComp : currComps) {
            DynGuiComponent component = convertVOtoGUIComp(voComp);
            component.setScreen(screen);
            component.setId(componentId++);

            voComp.setId(component.getId());
            cache.put(component.getId(), new ImmutablePair<>(voComp, component));
        }

        cache.forEach((id, pair) -> {
            DynGuiComponentVO voComp = pair.getKey();
            List<DynGuiComponentVO> voChildren = voComp.getChildren();
            List<DynGuiComponent> compChildren = voChildren.stream()
                    .map(vo -> cache.get(vo.getId()).getValue())
                    .collect(Collectors.toList());

            //set children and parent
            pair.getValue().setChildren(compChildren);
            DynGuiComponentVO parent = voComp.getParent();
            if (parent != null) pair.getValue().setParent(cache.get(parent.getId()).getValue());
        });

        return cache.values().stream()
                .map(Pair::getValue)
                .collect(Collectors.toList());
    }

    private static DynGuiComponent convertVOtoGUIComp(DynGuiComponentVO currComp) {
        DynGuiComponent component = new DynGuiComponent();

        component.setActivity(currComp.getActivity());
        component.setName(currComp.getName());
        component.setText(currComp.getText());
        component.setContentDescription(currComp.getContentDescription());
        component.setIdXml(currComp.getIdXml());
        component.setComponentIndex(currComp.getComponentIndex());
        component.setComponentTotalIndex(currComp.getComponentTotalIndex());
        component.setCurrentWindow(currComp.getCurrentWindow());
        component.setTitleWindow(currComp.getTitleWindow());
        component.setPositionX(currComp.getPositionX());
        component.setPositionY(currComp.getPositionY());
        component.setHeight(currComp.getHeight());
        component.setWidth(currComp.getWidth());
        component.setCheckable(currComp.isCheckable());
        component.setChecked(currComp.isChecked());
        component.setClickable(currComp.isClickable());
        component.setEnabled(currComp.isEnabled());
        component.setFocusable(currComp.isFocusable());
        component.setFocused(currComp.isFocused());
        component.setLongClickable(currComp.isLongClickable());
        component.setScrollable(currComp.isScrollable());
        component.setSelected(currComp.isSelected());
        component.setPassword(currComp.isPassword());
        component.setItemList(currComp.isItemList());
        component.setCalendarWindow(currComp.isCalendarWindow());
        component.setRelativeLocation(currComp.getRelativeLocation());
        component.setGuiScreenshot(currComp.getGuiScreenshot());
        component.setIdText(currComp.getIdText());
        component.setOffset(currComp.getOffset());
        component.setVisibility(currComp.getVisibility());
        component.setProperties(currComp.getProperties());
        component.setDrawTime(currComp.getDrawTime());

        return component;
    }
}
