package specman.model.v001;

import specman.Aenderungsart;

public class ImageEditAreaModel_V001 extends AbstractEditAreaModel_V001 {
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

  public ImageEditAreaModel_V001(byte[] imageData, String imageType, ChangeInfo_V001 changeInfo, float individualScalePercent) {
    this.imageData = imageData;
    this.imageType = imageType;
    this.aenderungsart = null;
    this.changeInfo = changeInfo;
    this.individualScalePercent = individualScalePercent;
  }
}
