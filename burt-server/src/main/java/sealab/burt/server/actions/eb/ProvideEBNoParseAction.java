package sealab.burt.server.actions.eb;

import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.entity.ChatBotMessage;
import sealab.burt.server.conversation.state.ConversationState;
import sealab.burt.server.msgparsing.Intent;

import java.util.List;

public class ProvideEBNoParseAction extends ChatBotAction {

    public ProvideEBNoParseAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

    @Override
    public List<ChatBotMessage> execute(ConversationState state){
        return createChatBotMessages("I am sorry, I couldn't recognize the language of your description",
                "Please tell me how the app is <b>supposed to work</b> one more time");
    }
}
