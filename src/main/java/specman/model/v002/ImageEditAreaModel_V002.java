package specman.model.v002;

import specman.ChangeInfo;

/** A bitmap picture, persisted as base64 encoded image data. */
public class ImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public final byte[] imageData;
    public final String imageType;

    @Deprecated public ImageEditAreaModel_V002() { // For Jackson only
        imageData = null;
        imageType = null;
    }

    public ImageEditAreaModel_V002(byte[] imageData, String imageType, float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.imageData = imageData;
        this.imageType = imageType;
    }
}
