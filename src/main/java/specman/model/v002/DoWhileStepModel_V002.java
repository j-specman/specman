package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

public class DoWhileStepModel_V002 extends WhileStepModel_V002 {

    @Deprecated public DoWhileStepModel_V002() {} // For Jackson only

    public DoWhileStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 loopSequence, int barWidth, String sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, stepNumber, content, color, changeInfo, collapsed, loopSequence, barWidth, sourceStepId, decorationStyle);
    }
}
