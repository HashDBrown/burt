package sealab.burt.server.actions.ob;

import sealab.burt.qualitychecker.QualityResult;
import sealab.burt.qualitychecker.graph.GraphState;
import sealab.burt.qualitychecker.graph.GraphTransition;
import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.ChatBotMessage;
import sealab.burt.server.conversation.KeyValues;
import sealab.burt.server.conversation.MessageObj;
import sealab.burt.server.msgparsing.Intent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static sealab.burt.server.StateVariable.OB_QUALITY_RESULT;
import static sealab.burt.server.actions.commons.ScreenshotPathUtils.getScreenshotPathForGraphState;

public class SelectOBScreenAction extends ChatBotAction {

    public SelectOBScreenAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

    public static List<KeyValues> getObScreenOptions(List<GraphState> matchedStates,
                                                     ConcurrentHashMap<StateVariable, Object> state) {
        int maxNumOfResults = 5;
        int initialResult = 0;
        return IntStream.range(initialResult, maxNumOfResults)
                .mapToObj(i -> {
                            if (matchedStates.size() <= i) return null;
                            GraphState graphState = matchedStates.get(i);

                            String description = GraphTransition.getWindowString(graphState.getScreen().getActivity(),
                                    graphState.getScreen().getWindow());
                            final int LIMIT_WINDOW_TEXT = 100;
                            if (description.length() > LIMIT_WINDOW_TEXT) {
                                description = description.substring(0, LIMIT_WINDOW_TEXT) + "...";
                            }

                            String screenshotFile = getScreenshotPathForGraphState(graphState, state);
                            return new KeyValues(Integer.toString(i), (i + 1) + ". " + description, screenshotFile);
                        }

                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatBotMessage> execute(ConcurrentHashMap<StateVariable, Object> state) {

        MessageObj messageObj = new MessageObj(
                " Please hit the \"Done\" button after you have selected it.", "OBScreenSelector");

        QualityResult result = (QualityResult) state.get(OB_QUALITY_RESULT);
        List<GraphState> matchedStates = result.getMatchedStates();

        List<KeyValues> options = getObScreenOptions(matchedStates, state);

        if (options.isEmpty())
            throw new RuntimeException("There are no options to show");

        return createChatBotMessages(
                "Got it. From the list below, can you please select the screen that is having the problem?",
                new ChatBotMessage(messageObj, options, true));
    }

}
