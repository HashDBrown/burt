package sealab.burt.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class ConversationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationController.class);
    ConcurrentHashMap<String, Object> conversations = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, List<MessageObj>> messages = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(ConversationController.class, args);
    }

    @PostMapping("/saveSingleMessage")
    public void saveSingleMessage(@RequestBody RequestMessage req) {
        String msg = "Saving the messages in the server...";
        LOGGER.debug(msg);
        List<MessageObj> sessionMsgs = messages.getOrDefault(req.getSessionId(), new ArrayList<>());
        sessionMsgs.add(req.messages.get(0));
        messages.put(req.getSessionId(), sessionMsgs);
    }

    @PostMapping("/saveMessages")
    public void saveMessages(@RequestBody RequestMessage req) {
        String msg = "Saving the messages in the server...";
        LOGGER.debug(msg);
        messages.put(req.getSessionId(), req.getMessages());
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
        conversations.putIfAbsent(sessionId, new Conversation(sessionId));
        return sessionId;
    }

    @PostMapping("/end")
    public String endConversation(@RequestParam(value = "id") String conversationId) {
        Object obj = conversations.remove(conversationId);
        return obj != null ? "true" : "false";
    }

}
