package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.UUID;

public abstract class StructuredStepModel_V002 extends AbstractStepModel_V002 {
    public final boolean collapsed;

    @Deprecated StructuredStepModel_V002() { // For Jackson only
        collapsed = false;
    }

    StructuredStepModel_V002(UUID id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, UUID sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, content, color, changeInfo, sourceStepId, decorationStyle);
        this.collapsed = collapsed;
    }
}
