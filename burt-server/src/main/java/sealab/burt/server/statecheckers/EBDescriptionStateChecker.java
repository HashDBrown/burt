package sealab.burt.server.statecheckers;

import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sealab.burt.qualitychecker.QualityResult;
import sealab.burt.qualitychecker.graph.GraphState;
import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ActionName;
import sealab.burt.server.output.BugReportElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static sealab.burt.server.StateVariable.*;
import static sealab.burt.server.actions.ActionName.*;

public class EBDescriptionStateChecker extends StateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(OBDescriptionStateChecker.class);

    private final static ConcurrentHashMap<String, ActionName> nextActions= new ConcurrentHashMap<>(){{
        put(QualityResult.Result.MATCH.name(), PROVIDE_S2R_FIRST);
        put(QualityResult.Result.NO_MATCH.name(), CLARIFY_EB);
        put(QualityResult.Result.NOT_PARSED.name(), PROVIDE_EB_NO_PARSE);
    }};

    public EBDescriptionStateChecker() {
        super(null);
    }

    @Override
    public ActionName nextAction(ConcurrentHashMap<StateVariable, Object> state) {
        try {

            List<BugReportElement> obReportElements = (List<BugReportElement>) state.get(REPORT_OB);

            BugReportElement bugReportElement = obReportElements.get(0);
            GraphState obState = (GraphState) bugReportElement.getOriginalElement();
            String obDescription = bugReportElement.getStringElement();

            QualityResult result = runEBQualityCheck(state, obState, obDescription);

            String description = "EB description";
            String screenshotPath = "app_logos/EBScreen.png";
            state.put(EB_DESCRIPTION, description);
            state.put(EB_SCREEN, screenshotPath);

            if (result.getResult().equals(QualityResult.Result.MATCH)){
//                UserMessage userMessage = (UserMessage) state.get(CURRENT_MESSAGE);
//                String message = userMessage.getMessages().get(0).getMessage();
                if (!state.containsKey(REPORT_EB)){

                    List<BugReportElement> outputMessageList = new ArrayList<>();
                    outputMessageList.add(new BugReportElement(description, null, screenshotPath));
                    state.put(REPORT_EB, outputMessageList);
                }else{
                    List<BugReportElement> outputMessage = (List<BugReportElement>) state.get(REPORT_EB);
                    outputMessage.add(new BugReportElement(description, null, screenshotPath));
                }
            }
            return nextActions.get(result.getResult().name());
        } catch (Exception e) {
            LOGGER.error("There was an error", e);
            return UNEXPECTED_ERROR;
        }
    }

}
