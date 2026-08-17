package specman.model.v002;

import specman.ChangeInfo;

import java.util.List;
import java.util.UUID;

public class CatchSequenceModel_V002 extends BranchSequenceModel_V002 {
    public final List<CoCatchModel_V002> coCatches;
    public final Integer headingRightBarWidth;

    @Deprecated public CatchSequenceModel_V002() { // For Jackson only
        coCatches = null;
        headingRightBarWidth = null;
    }

    public CatchSequenceModel_V002(UUID breakStepId, ChangeInfo changeInfo, EditorContentModel_V002 heading, List<CoCatchModel_V002> coCatches, Integer headingRightBarWidth) {
        super(breakStepId, changeInfo, null, heading);
        this.coCatches = coCatches;
        this.headingRightBarWidth = headingRightBarWidth;
    }
}
