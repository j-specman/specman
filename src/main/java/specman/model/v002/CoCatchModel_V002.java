package specman.model.v002;

import specman.ChangeInfo;

import java.util.UUID;

public class CoCatchModel_V002 {
    public final UUID breakStepId;
    public final EditorContentModel_V002 heading;
    public final ChangeInfoModel_V002 changeInfo;

    @Deprecated public CoCatchModel_V002() { // For Jackson only
        breakStepId = null;
        heading = null;
        changeInfo = null;
    }

    public CoCatchModel_V002(UUID breakStepId, EditorContentModel_V002 heading, ChangeInfo changeInfo) {
        this.breakStepId = breakStepId;
        this.heading = heading;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
    }
}
