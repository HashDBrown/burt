package sealab.burt.server.statecheckers;

import sealab.burt.server.StateVariable;
import sealab.burt.server.actions.ActionName;
import sealab.burt.server.conversation.UserResponse;
import seers.textanalyzer.TextProcessor;
import seers.textanalyzer.entity.Token;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static sealab.burt.server.StateVariable.*;

public class ParticipantIdStateChecker extends StateChecker {

    private static List<String> participants = IntStream.range(1, 30)
            .mapToObj(i -> "P" + i)
            .collect(Collectors.toList());

    public ParticipantIdStateChecker() {
        super(null);
    }

    @Override
    public ActionName nextAction(ConcurrentHashMap<StateVariable, Object> state) {

        UserResponse userResponse = (UserResponse) state.get(CURRENT_MESSAGE);
        String message = userResponse.getFirstMessage().getMessage();
        List<Token> tokens = TextProcessor.processText(message).get(0).getTokens();

        Optional<Token> token = tokens.stream().filter(tok -> participants.stream()
                .anyMatch(part -> tok.getWord().equalsIgnoreCase(part)))
                .findFirst();

        boolean validParticipant = token.isPresent();

        state.put(PARTICIPANT_VALIDATED, validParticipant);

        if (validParticipant) {
            state.put(PARTICIPANT_ID, token.get().getWord());
            return ActionName.SELECT_APP;
        } else {
            return ActionName.PROVIDE_PARTICIPANT_ID;
        }
    }
}
