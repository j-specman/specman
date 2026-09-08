package specman.model.v002;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
import java.util.Map;

public class SubsequenceStepModel_V002 extends StructuredStepModel_V002 {
    public final StepSequenceModel_V002 subsequence;
    public final boolean flatNumbering;

    @Deprecated public SubsequenceStepModel_V002() { // For Jackson only
        subsequence = null;
        flatNumbering = false;
    }

    public SubsequenceStepModel_V002(String id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 subsequence, String sourceStepId, RoundedBorderDecorationStyle decorationStyle, boolean flatNumbering) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.subsequence = subsequence;
        this.flatNumbering = flatNumbering;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        subsequence.addStepsRecursively(allSteps);
    }

    @Override public List<NumberedSubSequence_V002> subSequencesFor(StepNumber myNumber) {
        StepNumber base = flatNumbering ? myNumber.sameID() : myNumber.naechsteEbene();
        return List.of(new NumberedSubSequence_V002(subsequence, base));
    }

    @Override public StepNumber nextSlotInOuterSequence(StepNumber myNumber, Map<String, StepNumber> stepNumbers) {
        if (!flatNumbering) {
            return myNumber;
        }
        return flatLastNumber(subsequence, myNumber, stepNumbers);
    }

    private static StepNumber flatLastNumber(
            StepSequenceModel_V002 seq, StepNumber fallback, Map<String, StepNumber> stepNumbers) {
        if (seq.steps == null || seq.steps.isEmpty()) {
            return fallback;
        }
        AbstractStepModel_V002 lastStep = seq.steps.get(seq.steps.size() - 1);
        StepNumber lastNum = stepNumbers.get(lastStep.id);
        if (lastNum == null) {
            return fallback;
        }
        if (lastStep instanceof SubsequenceStepModel_V002 subStep && subStep.flatNumbering) {
            return flatLastNumber(subStep.subsequence, lastNum, stepNumbers);
        }
        return lastNum;
    }
}
