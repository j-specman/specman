package specman.model.v002;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;

import static specman.util.ObjectUtils.nvl;

public class ChangeInfoModel_V002 {
    public final Aenderungsart changetype;
    public final String changeset;

    @Deprecated public ChangeInfoModel_V002() { // For Jackson only
        this.changetype = null;
        this.changeset = null;
    }

    public ChangeInfoModel_V002(ChangeInfo changeInfo) {
        this.changetype = changeInfo.art();
        this.changeset = changeInfo.changeSetName();
    }

    public ChangeInfo toChangeInfo() {
        if (changetype == null || changetype == Aenderungsart.Untracked) {
            return ChangeInfo.UNTRACKED;
        }
        ChangeSet cs = ChangeSet.fromName(changeset);
        return new ChangeInfo(changetype, nvl(cs, ChangeSet.changeset()));
    }

    public static ChangeInfoModel_V002 from(ChangeInfo changeInfo) {
        return changeInfo != null ? new ChangeInfoModel_V002(changeInfo) : null;
    }
}
