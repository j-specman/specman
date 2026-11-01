package specman.model.v002;

import specman.ChangeInfo;

/** An imported SVG graphic, persisted as its source code. */
public class SvgImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "svg";

    public final String source;

    @Deprecated public SvgImageEditAreaModel_V002() { // For Jackson only
        source = null;
    }

    public SvgImageEditAreaModel_V002(String source, float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.source = source;
    }
}
