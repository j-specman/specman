package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

public class SimpleStepModel_V002 extends AbstractStepModel_V002 {

    @Deprecated public SimpleStepModel_V002() {} // For Jackson only

    public SimpleStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, String sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, stepNumber, content, color, changeInfo, sourceStepId, decorationStyle);
    }
}
