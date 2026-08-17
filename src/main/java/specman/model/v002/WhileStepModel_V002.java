package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
import java.util.UUID;

public class WhileStepModel_V002 extends StructuredStepModel_V002 {
    public final StepSequenceModel_V002 loopSequence;
    public final int barWidth;

    @Deprecated public WhileStepModel_V002() { // For Jackson only
        loopSequence = null;
        barWidth = 0;
    }

    public WhileStepModel_V002(UUID id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 loopSequence, int barWidth, UUID sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.loopSequence = loopSequence;
        this.barWidth = barWidth;
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        loopSequence.addStepsRecursively(allSteps);
    }
}
