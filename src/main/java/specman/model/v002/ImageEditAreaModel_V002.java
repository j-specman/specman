package specman.model.v002;

import specman.ChangeInfo;

public class ImageEditAreaModel_V002 extends AbstractEditAreaModel_V002 {
    public final byte[] imageData;
    public final String imageType;
    public final float individualScalePercent;
    public final ChangeInfoModel_V002 changeInfo;

    @Deprecated public ImageEditAreaModel_V002() { // For Jackson only
        imageData = null;
        imageType = null;
        individualScalePercent = 0;
        changeInfo = null;
    }

    public ImageEditAreaModel_V002(byte[] imageData, String imageType, float individualScalePercent, ChangeInfo changeInfo) {
        this.imageData = imageData;
        this.imageType = imageType;
        this.individualScalePercent = individualScalePercent;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
    }
}
