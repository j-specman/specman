package specman.model.v002;

import specman.ChangeInfo;

/** A Classman class diagram, persisted as its source text. The picture is laid out and rendered from the source when
 * loading. Change marks are part of the source. */
public class ClassmanImageEditAreaModel_V002 extends AbstractSourceImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "classman";

    @Deprecated public ClassmanImageEditAreaModel_V002() { // For Jackson only
    }

    public ClassmanImageEditAreaModel_V002(String source, float individualScalePercent, ChangeInfo changeInfo) {
        super(source, individualScalePercent, changeInfo);
    }
}
