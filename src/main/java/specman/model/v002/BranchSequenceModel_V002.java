package specman.model.v002;

import specman.ChangeInfo;

public class BranchSequenceModel_V002 extends StepSequenceModel_V002 {
    public final EditorContentModel_V002 heading;

    @Deprecated public BranchSequenceModel_V002() { // For Jackson only
        heading = null;
    }

    public BranchSequenceModel_V002(String id, ChangeInfo changeInfo, CatchAreaModel_V002 catchArea, EditorContentModel_V002 heading) {
        super(id, changeInfo, catchArea);
        this.heading = heading;
    }
}
