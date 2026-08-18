package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.StepNumber;

public class CoCatchModel_V001 implements ChangeInfoBackwardsCompatible_V001 {
  public final StepNumber breakStepId;
  public final EditorContentModel_V001 heading;
  public final Aenderungsart changetype; // kept for backwards compatibility
  public final ChangeInfo_V001 changeInfo;

  public CoCatchModel_V001() {
    this.breakStepId = null;
    this.heading = null;
    this.changetype = null;
    this.changeInfo = null;
  }

  public CoCatchModel_V001(StepNumber breakStepId, EditorContentModel_V001 heading, ChangeInfo changeInfo) {
    this.breakStepId = breakStepId;
    this.heading = heading;
    this.changetype = asLegacyAenderungsart(changeInfo);
    this.changeInfo = asChangeInfo(changeInfo);
  }
}
