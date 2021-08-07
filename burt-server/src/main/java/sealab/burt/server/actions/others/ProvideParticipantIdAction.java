package sealab.burt.server.actions.others;

import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.entity.ChatBotMessage;
import sealab.burt.server.conversation.state.ConversationState;
import sealab.burt.server.msgparsing.Intent;

import java.util.List;

import static sealab.burt.server.StateVariable.PARTICIPANT_ASKED;
import static sealab.burt.server.StateVariable.PARTICIPANT_VALIDATED;

public class ProvideParticipantIdAction extends ChatBotAction {

    public ProvideParticipantIdAction(Intent nextIntent) {
        super(nextIntent);
    }

    @Override
    public List<ChatBotMessage> execute(ConversationState state) throws Exception {

        Boolean validParticipant = (Boolean) state.get(PARTICIPANT_VALIDATED);

        state.put(PARTICIPANT_ASKED, true);

        if (validParticipant == null)
            return createChatBotMessages(
                    "Before we start, let me remind you that at this moment you should <b>start a " +
                            "stopwatch from scratch</b> to time yourself during the reporting session",
                    "Also, let me remind you that some of the <b>screenshots</b> that I will display are <b>for " +
                            "reference only</b>",
                    "<b>Input values</b> and <b>UI components</b> may be a little different from what you " +
                            "actually observed in the app",
                    "To start, please provide the <b>Participant ID</b> that was assigned to " +
                            "you (e.g., P3)");
        else
            return createChatBotMessages("Sorry, I couldn't identify the <b>Participant ID</b>, " +
                    "please provide it one more time (e.g., P3)");
    }
}
