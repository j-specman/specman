package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class StructuredStepModel_V002 extends AbstractStepModel_V002 {
    public final boolean collapsed;

    @Deprecated StructuredStepModel_V002() { // For Jackson only
        collapsed = false;
    }

    StructuredStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, Integer shade, ChangeInfo changeInfo, boolean collapsed, String sourceStepId, RoundedBorderDecorationStyle decorationStyle, List<MetaTagModel_V002> tags) {
        super(id, stepNumber, content, shade, changeInfo, sourceStepId, decorationStyle, tags);
        this.collapsed = collapsed;
    }
}
