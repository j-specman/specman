package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;

public class ChangeInfo_V001 {
  public final Aenderungsart changetype;
  public final String changeset;

  public ChangeInfo_V001() { // For Jackson only
    this.changetype = null;
    this.changeset = null;
  }

  public ChangeInfo_V001(ChangeInfo changeInfo) {
    this.changetype = changeInfo.art();
    this.changeset = changeInfo.changeSetName();
  }

  public ChangeInfo toChangeInfo() {
    if (changetype == null || changetype == Aenderungsart.Untracked) {
      return ChangeInfo.UNTRACKED;
    }
    ChangeSet cs = ChangeSet.fromName(changeset);
    return new ChangeInfo(changetype, cs != null ? cs : ChangeSet.changeset());
  }
}
