package specman.model.v002;

import specman.ChangeInfo;

/** What all areas showing a picture have in common, no matter how the picture is represented. */
public abstract class AbstractImageEditAreaModel_V002 extends AbstractEditAreaModel_V002 {
    public final float individualScalePercent;

    protected AbstractImageEditAreaModel_V002() { // For Jackson only
        individualScalePercent = 0;
        changeInfo = null;
    }

    protected AbstractImageEditAreaModel_V002(float individualScalePercent, ChangeInfo changeInfo) {
        this.individualScalePercent = individualScalePercent;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
    }
}
