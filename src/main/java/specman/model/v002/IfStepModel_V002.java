package specman.model.v002;

import specman.ChangeInfo;
import specman.Specman;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
import java.util.UUID;

import static specman.Specman.editor;

public class IfStepModel_V002 extends StructuredStepModel_V002 {
    public final BranchSequenceModel_V002 ifSequence;
    public final int emptyWidth;

    @Deprecated public IfStepModel_V002() { // For Jackson only
        ifSequence = new BranchSequenceModel_V002();
        emptyWidth = 20 * editor().getZoomFactor() / 100;
    }

    public IfStepModel_V002(UUID id, EditorContentModel_V002 content, int color, RoundedBorderDecorationStyle decorationStyle, boolean collapsed, ChangeInfo changeInfo, BranchSequenceModel_V002 ifSequence, int emptyWidth, UUID sourceStepId) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.ifSequence = ifSequence;
        this.emptyWidth = 20 * editor().getZoomFactor() / 100;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        ifSequence.addStepsRecursively(allSteps);
    }
}
