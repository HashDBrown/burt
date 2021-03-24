package sealab.burt.server.actions.expectedbehavior;

import sealab.burt.qualitychecker.EBChecker;
import sealab.burt.server.ChatbotMessage;
import sealab.burt.server.actions.ChatbotAction;

import java.util.concurrent.ConcurrentHashMap;

public class ProvideEBAction extends ChatbotAction {
    @Override
    public ChatbotMessage execute(ConcurrentHashMap<String, Object> state) {
        state.put("OB_COLLECTED", true);
        state.put("COLLECTING_EB", true);
        String appName = state.get("APP").toString();
        String appVersion = state.get("APP_VERSION").toString();
        if (!state.containsKey("EB_CHECKER")) state.put("EB_CHECKER", new EBChecker(appName, appVersion));
        return new ChatbotMessage("ok, can you please tell me how the app is supposed to work instead?");
    }

    @Override
    public String nextExpectedIntent() {
        return "EB_DESCRIPTION";
    }
}
