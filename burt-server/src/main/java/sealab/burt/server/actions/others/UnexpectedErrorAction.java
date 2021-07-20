package sealab.burt.server.actions.others;

import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.ChatBotMessage;
import sealab.burt.server.conversation.ConversationState;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UnexpectedErrorAction extends ChatBotAction {

    @Override
    public List<ChatBotMessage> execute(ConversationState state){
        return createChatBotMessages("I am sorry, there was an unexpected internal error.",
                "Please try again or restart the conversation.");
    }

}
