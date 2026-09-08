package specman.model.v002;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class WhileStepModel_V002 extends StructuredStepModel_V002 {
    public final StepSequenceModel_V002 loopSequence;
    public final int barWidth;

    @Deprecated public WhileStepModel_V002() { // For Jackson only
        loopSequence = null;
        barWidth = 0;
    }

    public WhileStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 loopSequence, int barWidth, String sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, stepNumber, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.loopSequence = loopSequence;
        this.barWidth = barWidth;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        loopSequence.addStepsRecursively(allSteps);
    }

    @Override public List<NumberedSubSequence_V002> subSequencesFor(StepNumber myNumber) {
        return List.of(new NumberedSubSequence_V002(loopSequence, myNumber.naechsteEbene()));
    }
}
