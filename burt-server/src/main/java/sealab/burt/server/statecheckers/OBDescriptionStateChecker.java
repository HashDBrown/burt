package sealab.burt.server.statecheckers;

import lombok.extern.slf4j.Slf4j;
import sealab.burt.qualitychecker.QualityResult;
import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ActionName;
import sealab.burt.server.conversation.ConversationState;
import sealab.burt.server.conversation.UserResponse;

import java.util.concurrent.ConcurrentHashMap;

import static sealab.burt.server.StateVariable.CURRENT_MESSAGE;
import static sealab.burt.server.StateVariable.OB_DESCRIPTION;
import static sealab.burt.server.actions.ActionName.*;

public @Slf4j
class OBDescriptionStateChecker extends StateChecker {

    private static final ConcurrentHashMap<String, ActionName> nextActions = new ConcurrentHashMap<>() {{
        put(QualityResult.Result.MATCH.name(), PROVIDE_EB);
        put(QualityResult.Result.MULTIPLE_MATCH.name(), SELECT_OB_SCREEN);
        put(QualityResult.Result.NO_MATCH.name(), REPHRASE_OB);
        put(QualityResult.Result.NOT_PARSED.name(), PROVIDE_OB_NO_PARSE);
    }};

    public OBDescriptionStateChecker() {
        super(null);
    }

    @Override
    public ActionName nextAction(ConversationState state) {
        try {
            QualityResult result = runOBQualityCheck(state);
            UserResponse userResponse = (UserResponse) state.get(CURRENT_MESSAGE);

            state.put(OB_DESCRIPTION, userResponse.getFirstMessage().getMessage());

            if (result.getResult().equals(QualityResult.Result.MATCH)) {
                QualityStateUpdater.updateOBState(state, result.getMatchedStates().get(0));
            }

            return nextActions.get(result.getResult().name());
        } catch (Exception e) {
            log.error("There was an error", e);
            return UNEXPECTED_ERROR;
        }
    }

}
