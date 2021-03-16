package sealab.burt.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sealab.burt.server.actions.*;
import sealab.burt.server.actions.appselect.ConfirmAppAction;
import sealab.burt.server.actions.appselect.SelectAppAction;
import sealab.burt.server.actions.expectedbehavior.ClarifyEBAction;
import sealab.burt.server.actions.expectedbehavior.ProvideEBAction;
import sealab.burt.server.actions.observedbehavior.ConfirmOBScreenSelectedAction;
import sealab.burt.server.actions.observedbehavior.ProvideOBAction;
import sealab.burt.server.actions.observedbehavior.RephraseOBAction;
import sealab.burt.server.actions.observedbehavior.SelectOBScreenAction;
import sealab.burt.server.actions.step2reproduce.*;
import sealab.burt.server.statecheckers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class ConversationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationController.class);
    ConcurrentHashMap<String, List<MessageObj>> messages = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> conversationStates = new ConcurrentHashMap<>();
    //    HashMap<String,
    ConcurrentHashMap<String, ChatbotAction> actions = new ConcurrentHashMap<>() {
        {
            put("SELECT_APP", new SelectAppAction());
            put("CONFIRM_APP", new ConfirmAppAction());

            //--------OB---------------//
            put("PROVIDE_OB", new ProvideOBAction());
            put("REPHRASE_OB", new RephraseOBAction());
            put("SELECT_OB_SCREEN", new SelectOBScreenAction());
            put("CONFIRM_SELECTED_OB_SCREEN", new ConfirmOBScreenSelectedAction());

            //--------EB-------------//
            put("PROVIDE_EB", new ProvideEBAction());
            put("CLARIFY_EB", new ClarifyEBAction());

            //--------S2R-----------//
            put("PROVIDE_S2R_FIRST", new ProvideS2RFirstAction());
            put("PREDICT_S2R", new ProvidePredictedS2RAction());
            put("PROVIDE_S2R", new ProvideS2RAction());
            put("DISAMBIGUATE_S2R", new DisambiguateS2RAction());
            put("REPHRASE_S2R", new RephraseS2RAction());
            put("SPECIFY_INPUT_S2R", new SpecifyInputS2RAction());
            put("SELECT_MISSING_S2R", new SelectMissingS2RAction());
            put("CONFIRM_SELECTED_AMBIGUOUS_S2R", new ConfirmSelectedAmbiguousAction());
            put("CONFIRM_LAST_STEP", new ConfirmLastStepAction());
            put("REPORT_SUMMARY", new ProvideReportSummary());


        }
    };
    ConcurrentHashMap<String, StateChecker> stateCheckers = new ConcurrentHashMap<>() {{
        put("GREETING", new NStateChecker("SELECT_APP"));
        put("APP_SELECTED", new NStateChecker("CONFIRM_APP"));
        put("AFFIRMATIVE_ANSWER", new AffirmativeAnswerStateChecker(null));
//                "GREETING": new NoStateChecker("SELECT_APP"),
//            "APP_SELECTED": new NoStateChecker("CONFIRM_APP"),
//            "AFFIRMATIVE_ANSWER": new AffirmativeAnswerStateChecker(null)

        //--------OB---------------//
        put("OB_DESCRIPTION", new OBDescriptionStateChecker(null));
        put("OB_SCREEN_SELECTED", new NStateChecker("CONFIRM_SELECTED_OB_SCREEN"));
        //--------EB-------------//
        put("EB_DESCRIPTION", new EBDescriptionStateChecker(null));
        //--------S2R-----------//
        put("S2R_DESCRIPTION", new S2RDescriptionStateChecker(null));
        put("S2R_PREDICTED_SELECTED", new S2RDescriptionStateChecker(null));
        put("S2R_MISSING_SELECTED", new S2RDescriptionStateChecker(null));
        put("S2R_AMBIGUOUS_SELECTED", new NStateChecker("CONFIRM_SELECTED_AMBIGUOUS_S2R"));

    }};


    public static void main(String[] args) {
        SpringApplication.run(ConversationController.class, args);
    }


    @PostMapping("/processMessage")
    public ConversationResponse processMessage(@RequestBody RequestMessage req) {
        MessageObj message = req.getMessages().get(0);
        String sessionId = req.getSessionId();

        ConcurrentHashMap<String, Object> state = conversationStates.get(sessionId);
        state.put("CURRENT_MESSAGE", message);

        System.out.println(state.get("CONVERSATION_STATE"));
        String intent = MessageParser.getIntent(message, state);

        if (intent == null)
            return ConversationResponse.createResponse("Sorry, I did not get that!");


        StateChecker stateChecker = stateCheckers.get(intent);
        if (stateChecker == null)
            return ConversationResponse.createResponse("Sorry, I am not sure how to respond in this case");

        ChatbotAction nextAction = actions.get(stateChecker.nextAction(state));
        System.out.println(state.get("CONVERSATION_STATE"));

        if (nextAction == null)
            return ConversationResponse.createResponse("Sorry, I am not sure what to do in this case");

        String nextIntent = nextAction.nextExpectedIntent();
        MessageObj nextMessage = nextAction.execute(state);

        state.put("NEXT_INTENT", nextIntent);

        return new ConversationResponse(new ChatbotMessage(nextMessage), 0);
    }


    @PostMapping("/saveSingleMessage")
    public void saveSingleMessage(@RequestBody RequestMessage req) {
        String msg = "Saving the messages in the server...";
        LOGGER.debug(msg);
        List<MessageObj> sessionMsgs = messages.getOrDefault(req.getSessionId(), new ArrayList<>());
        sessionMsgs.add(req.getMessages().get(0));
        messages.put(req.getSessionId(), sessionMsgs);
    }

    @PostMapping("/saveMessages")
    public void saveMessages(@RequestBody RequestMessage req) {
        String msg = "Saving the messages in the server...";
        LOGGER.debug(msg);
        messages.put(req.getSessionId(), req.getMessages());
    }

    @PostMapping("/testResponse")
    public ConversationResponse testResponse(@RequestBody RequestMessage req) {
        MessageObj responseMessageObj = req.getMessages().get(0);
        return ConversationResponse.createResponse(responseMessageObj.getMessage());

    }

    @PostMapping("/loadMessages")
    public List<MessageObj> loadMessages(@RequestBody RequestMessage req) {
        String msg = "Returning the messages in the server...";
        LOGGER.debug(msg);
        return messages.get(req.getSessionId());
    }

    @PostMapping("/")
    public String index() {
        String msg = "BURT is running...";
        LOGGER.debug(msg);
        return msg;
    }

    @PostMapping("/echo")
    public ConversationResponse echo() {
        LOGGER.debug("Echoing");
        return ConversationResponse.createResponse("BURT is running");
    }

    @PostMapping("/start")
    public String startConversation() {
        String sessionId = UUID.randomUUID().toString();
        conversationStates.putIfAbsent(sessionId, new ConcurrentHashMap<>());
        return sessionId;
    }

    @PostMapping("/end")
    public String endConversation(@RequestParam(value = "id") String conversationId) {
        Object obj = conversationStates.remove(conversationId);
        return obj != null ? "true" : "false";
    }

}
