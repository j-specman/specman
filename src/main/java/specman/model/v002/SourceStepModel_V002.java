package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class SourceStepModel_V002 extends AbstractStepModel_V002 {

    @Deprecated public SourceStepModel_V002() {} // For Jackson only

    public SourceStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, Integer shade, ChangeInfo changeInfo, String sourceStepId, RoundedBorderDecorationStyle decorationStyle, List<MetaTagModel_V002> tags) {
        super(id, stepNumber, content, shade, changeInfo, sourceStepId, decorationStyle, tags);
    }
}
