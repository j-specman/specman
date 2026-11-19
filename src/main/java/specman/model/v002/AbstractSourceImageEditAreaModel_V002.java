package specman.model.v002;

import specman.ChangeInfo;

/** An image area which shows a diagram rendered from its source, which is persisted as text. */
public abstract class AbstractSourceImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public final String source;

    protected AbstractSourceImageEditAreaModel_V002() { // For Jackson only
        source = null;
    }

    protected AbstractSourceImageEditAreaModel_V002(String source, float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.source = source;
    }
}
