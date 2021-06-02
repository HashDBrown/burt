package sealab.burt.server.actions.ob;


import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.ChatBotMessage;
import sealab.burt.server.msgparsing.Intent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class RephraseOBAction extends ChatBotAction {

    public RephraseOBAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

    @Override
    public List<ChatBotMessage> execute(ConcurrentHashMap<StateVariable, Object> state){
        return createChatBotMessages("It seems the description you provided does not use a proper language.",
                "Can you please rephrase the incorrect behavior?");
    }

}
