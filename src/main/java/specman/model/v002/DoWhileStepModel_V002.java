package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class DoWhileStepModel_V002 extends WhileStepModel_V002 {

    @Deprecated public DoWhileStepModel_V002() {} // For Jackson only

    public DoWhileStepModel_V002(String id, String stepNumber, EditorContentModel_V002 content, Integer shade, ChangeInfo changeInfo, boolean collapsed, StepSequenceModel_V002 loopSequence, int barWidth, String sourceStepId, RoundedBorderDecorationStyle decorationStyle, List<MetaTagModel_V002> tags) {
        super(id, stepNumber, content, shade, changeInfo, collapsed, loopSequence, barWidth, sourceStepId, decorationStyle, tags);
    }
}
