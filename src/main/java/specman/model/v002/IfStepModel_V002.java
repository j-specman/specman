package specman.model.v002;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class IfStepModel_V002 extends StructuredStepModel_V002 {
    public final BranchSequenceModel_V002 ifSequence;
    public final int emptyWidth;

    @Deprecated public IfStepModel_V002() { // For Jackson only
        ifSequence = new BranchSequenceModel_V002();
        emptyWidth = 0;
    }

    public IfStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, int color, RoundedBorderDecorationStyle decorationStyle, boolean collapsed, ChangeInfo changeInfo, BranchSequenceModel_V002 ifSequence, int emptyWidth, String sourceStepId) {
        super(id, stepNumber, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.ifSequence = ifSequence;
        this.emptyWidth = emptyWidth;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        ifSequence.addStepsRecursively(allSteps);
    }

    @Override public List<NumberedSubSequence_V002> subSequencesFor(StepNumber myNumber) {
        return List.of(new NumberedSubSequence_V002(ifSequence, myNumber.naechsteEbene()));
    }
}
