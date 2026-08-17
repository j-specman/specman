package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.UUID;

public class SourceStepModel_V002 extends AbstractStepModel_V002 {

    @Deprecated public SourceStepModel_V002() {} // For Jackson only

    public SourceStepModel_V002(UUID id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, UUID sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, content, color, changeInfo, sourceStepId, decorationStyle);
    }
}
