package sealab.burt.server.statecheckers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sealab.burt.qualitychecker.QualityResult;
import sealab.burt.qualitychecker.S2RChecker;
import sealab.burt.server.UserMessage;

import java.util.concurrent.ConcurrentHashMap;

import static sealab.burt.qualitychecker.QualityResult.Result.MULTIPLE_MATCH;
import static sealab.burt.qualitychecker.QualityResult.Result.MATCH;

public class S2RDescriptionStateChecker extends StateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OBDescriptionStateChecker.class);

    private static final ConcurrentHashMap<String, String> nextActions = new ConcurrentHashMap<>() {{
        put(QualityResult.Result.MATCH.name(), "PREDICT_S2R");
        put(QualityResult.Result.MULTIPLE_MATCH.name(), "DISAMBIGUATE_S2R");
        put(QualityResult.Result.NO_MATCH.name(), "REPHRASE_S2R");
        put(QualityResult.Result.NO_S2R_INPUT.name(), "SPECIFY_INPUT_S2R");
        put(QualityResult.Result.MISSING_STEPS.name(), "SELECT_MISSING_S2R");
    }};

    public S2RDescriptionStateChecker(String defaultAction) {
        super(defaultAction);
    }

    @Override
    public String nextAction(ConcurrentHashMap<String, Object> state) {

        try {
//            UserMessage userMessage = (UserMessage) state.get("CURRENT_MESSAGE");
//            S2RChecker checker = (S2RChecker) state.get("S2R_CHECKER");
//            QualityResult result = checker.checkS2R(userMessage.getMessages().get(0).getMessage());


//            QualityResult result =  new QualityResult(MATCH) ; // test action PREDICT_S2R
//            state.put("S2R_QUALITY_RESULT", result);


            QualityResult result =  new QualityResult(MULTIPLE_MATCH) ; // test action "DISAMBIGUATE_S2R"
            state.put("S2R_QUALITY_RESULT", result);

            return nextActions.get(result.getResult().name());
        } catch (Exception e) {
            LOGGER.error("There was an error", e);
            return null;
        }

    }
}
