package sealab.burt.server.actions.step2reproduce;

import sealab.burt.server.MessageObj;
import sealab.burt.server.actions.ChatbotAction;

import java.util.concurrent.ConcurrentHashMap;

public class ProvideS2RAction extends ChatbotAction {
    @Override
    public MessageObj execute(ConcurrentHashMap<String, Object> state) {
        return new MessageObj(" Ok, what is the next step?");
        //provide screenshots here
    }

    @Override
    public String nextExpectedIntent() {
        return "S2R_DESCRIPTION";
    }
}
