package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

public class StructuredStepModel_V002 extends AbstractStepModel_V002 {
    public final boolean collapsed;

    @Deprecated StructuredStepModel_V002() { // For Jackson only
        collapsed = false;
    }

    StructuredStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, String sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, stepNumber, content, color, changeInfo, sourceStepId, decorationStyle);
        this.collapsed = collapsed;
    }
}
