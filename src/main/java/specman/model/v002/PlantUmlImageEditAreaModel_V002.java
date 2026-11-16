package specman.model.v002;

import specman.ChangeInfo;

/** A PlantUML diagram, persisted as its source code. The picture is rendered from the source when loading. */
public class PlantUmlImageEditAreaModel_V002 extends AbstractSourceImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "plantuml";

    @Deprecated public PlantUmlImageEditAreaModel_V002() { // For Jackson only
    }

    public PlantUmlImageEditAreaModel_V002(String source, String backupSource, String backupChangeset,
                                           float individualScalePercent, ChangeInfo changeInfo) {
        super(source, backupSource, backupChangeset, individualScalePercent, changeInfo);
    }
}
