package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.SchrittID;

public class CoCatchModel_V001 {
  public final SchrittID breakStepId;
  public final EditorContentModel_V001 heading;
  public final Aenderungsart changetype; // kept for backwards compatibility
  public final ChangeInfo_V001 changeInfo;

  public CoCatchModel_V001() {
    this.breakStepId = null;
    this.heading = null;
    this.changetype = null;
    this.changeInfo = null;
  }

  public CoCatchModel_V001(SchrittID breakStepId, EditorContentModel_V001 heading, ChangeInfo changeInfo) {
    this.breakStepId = breakStepId;
    this.heading = heading;
    this.changetype = null;
    this.changeInfo = new ChangeInfo_V001(changeInfo);
  }
}
