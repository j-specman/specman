package specman.model.v002;

import specman.ChangeInfo;

/** A PlantUML diagram, persisted as its source code. The picture is rendered from the source when loading. */
public class PlantUmlImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "plantuml";

    public final String source;

    @Deprecated public PlantUmlImageEditAreaModel_V002() { // For Jackson only
        source = null;
    }

    public PlantUmlImageEditAreaModel_V002(String source, float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.source = source;
    }
}
