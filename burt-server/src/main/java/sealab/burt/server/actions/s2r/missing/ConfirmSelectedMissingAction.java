package sealab.burt.server.actions.s2r.missing;

import sealab.burt.qualitychecker.graph.AppStep;
import sealab.burt.qualitychecker.s2rquality.QualityFeedback;
import sealab.burt.qualitychecker.s2rquality.S2RQualityAssessment;
import sealab.burt.qualitychecker.s2rquality.S2RQualityCategory;
import sealab.burt.server.actions.ChatBotAction;
import sealab.burt.server.conversation.entity.*;
import sealab.burt.server.conversation.state.ConversationState;
import sealab.burt.server.msgparsing.Intent;
import sealab.burt.server.conversation.state.QualityStateUpdater;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static sealab.burt.server.StateVariable.*;
import static sealab.burt.server.msgparsing.Intent.S2R_DESCRIPTION;
import static sealab.burt.server.msgparsing.Intent.S2R_MISSING_SELECTED;

public class ConfirmSelectedMissingAction extends ChatBotAction {

    public ConfirmSelectedMissingAction(Intent nextExpectedIntent) {
        super(nextExpectedIntent);
    }

    @Override
    public List<ChatBotMessage> execute(ConversationState state) {
        UserResponse currentMessage = (UserResponse) state.get(CURRENT_MESSAGE);

        @SuppressWarnings("unchecked") final List<AppStep> allMissingSteps = (List<AppStep>) state.get(S2R_ALL_MISSING);

        if (currentMessage.getMessages().isEmpty()) {
            return getDefaultMessage(allMissingSteps, state);
        }

        //-------------------------------

        QualityFeedback feedback = (QualityFeedback) state.get(S2R_QUALITY_RESULT);
        S2RQualityAssessment assessment = feedback.getQualityAssessments().stream()
                .filter(qa -> qa.getCategory().equals(S2RQualityCategory.HIGH_QUALITY))
                .findFirst().orElse(null);

        if (assessment == null)
            throw new RuntimeException("The high quality assessment is required");


        UserResponse highQualityMessage = (UserResponse) state.get(S2R_MATCHED_MSG);
        String highQualityStepMessage = highQualityMessage.getMessages().get(0).getMessage();

        //------------------------------
        this.nextExpectedIntents = Collections.singletonList(S2R_DESCRIPTION);

        MessageObj message = currentMessage.getFirstMessage();
        if (DONE.equals(message.getMessage())) {

            List<String> selectedValues = message.getSelectedValues();

            List<AppStep> selectedSteps = selectedValues.stream()
                    .map(selectedValue -> allMissingSteps.get(Integer.parseInt(selectedValue)))
                    .collect(Collectors.toList());

        /*    List<AppStep> selectedSteps = allMissingSteps.stream()
                    .filter(step -> selectedValues.contains(step.getId().toString()))
                    .collect(Collectors.toList());*/

            if (selectedSteps.isEmpty() || selectedValues.size() != selectedSteps.size())
                return getDefaultMessage(allMissingSteps, state);

            QualityStateUpdater.addStepsToState(state, selectedSteps);

            QualityStateUpdater.addStepAndUpdateGraphState(state, highQualityStepMessage, assessment);

            //---------------------

            StringBuilder msg1 = new StringBuilder();
            msg1.append("Okay, you selected ")
                    .append(selectedSteps.size())
                    .append(" prior step(s).");

            StringBuilder msg2 = new StringBuilder();
            msg2.append("What step did you perform after the step \"")
                    .append(highQualityStepMessage)
                    .append("\"?");

            return createChatBotMessages(msg1.toString(), msg2.toString());

        } else if (NONE.equals(message.getMessage())) {

            QualityStateUpdater.addStepAndUpdateGraphState(state, highQualityStepMessage, assessment);

            return createChatBotMessages("Got it, what is the next step?");
        } else {
            return getDefaultMessage(allMissingSteps, state);
        }

    }

    private List<ChatBotMessage> getDefaultMessage(List<AppStep> allMissingSteps,
                                                   ConversationState state) {
        this.nextExpectedIntents = Collections.singletonList(S2R_MISSING_SELECTED);
        List<KeyValues> stepOptions = SelectMissingS2RAction.getStepOptions(allMissingSteps, state);

        MessageObj messageObj = new MessageObj(
                "From the following options, select the steps you performed before this step",
                WidgetName.S2RScreenSelector);

        return createChatBotMessages(
                "Sorry, the options you selected are incorrect.",
                new ChatBotMessage(messageObj, stepOptions, true));
    }


}
