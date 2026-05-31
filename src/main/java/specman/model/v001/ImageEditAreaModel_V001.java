package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;

public class ImageEditAreaModel_V001 extends AbstractEditAreaModel_V001 implements ChangeInfoBackwardsCompatible_V001 {
  public final byte[] imageData;
  public final String imageType;
  public final Aenderungsart aenderungsart; // kept for backwards compatibility
  public final ChangeInfo_V001 changeInfo;
  public final float individualScalePercent;

  public ImageEditAreaModel_V001() { // For Jackson only
    this.imageData = null;
    this.imageType = null;
    this.aenderungsart = null;
    this.changeInfo = null;
    this.individualScalePercent = 0;
  }

  public ImageEditAreaModel_V001(byte[] imageData, String imageType, ChangeInfo changeInfo, float individualScalePercent) {
    this.imageData = imageData;
    this.imageType = imageType;
    this.aenderungsart = asLegacyAenderungsart(changeInfo);
    this.changeInfo = asChangeInfo(changeInfo);
    this.individualScalePercent = individualScalePercent;
  }
}
