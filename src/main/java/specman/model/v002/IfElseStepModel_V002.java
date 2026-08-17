package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
import java.util.UUID;

public class IfElseStepModel_V002 extends StructuredStepModel_V002 {
    public final BranchSequenceModel_V002 ifSequence;
    public final BranchSequenceModel_V002 elseSequence;
    public final float ifWidthRatio;

    @Deprecated public IfElseStepModel_V002() { // For Jackson only
        ifSequence = new BranchSequenceModel_V002();
        elseSequence = new BranchSequenceModel_V002();
        ifWidthRatio = 0.0f;
    }

    public IfElseStepModel_V002(UUID id, EditorContentModel_V002 content, int color, RoundedBorderDecorationStyle decorationStyle, boolean collapsed, ChangeInfo changeInfo, BranchSequenceModel_V002 ifSequence, BranchSequenceModel_V002 elseSequence, float ifWidthRatio, UUID sourceStepId) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.ifSequence = ifSequence;
        this.elseSequence = elseSequence;
        this.ifWidthRatio = ifWidthRatio;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        ifSequence.addStepsRecursively(allSteps);
        elseSequence.addStepsRecursively(allSteps);
    }
}
