package sealab.burt.server.actions.s2r.input;

import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.entity.ChatBotMessage;
import sealab.burt.server.conversation.state.ConversationState;
import sealab.burt.server.msgparsing.Intent;

import java.util.List;

public class SpecifyInputS2RAction extends ChatBotAction {

    public SpecifyInputS2RAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

    @Override
    public List<ChatBotMessage> execute(ConversationState state) {
        return createChatBotMessages("It seems that no specific input or value was provided for the step",
                "Please provide an input (e.g., 5)");
    }

}
