package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
import java.util.UUID;

public class SubsequenceStepModel_V002 extends StructuredStepModel_V002 {
    public final StepSequenceModel_V002 subsequence;
    public final boolean flatNumbering;

    @Deprecated public SubsequenceStepModel_V002() { // For Jackson only
        subsequence = null;
        flatNumbering = false;
    }

    public SubsequenceStepModel_V002(UUID id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 subsequence, UUID sourceStepId, RoundedBorderDecorationStyle decorationStyle, boolean flatNumbering) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.subsequence = subsequence;
        this.flatNumbering = flatNumbering;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        subsequence.addStepsRecursively(allSteps);
    }
}
