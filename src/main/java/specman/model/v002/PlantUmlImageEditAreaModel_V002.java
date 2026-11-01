package specman.model.v002;

import specman.ChangeInfo;

/** A PlantUML diagram, persisted as its source code. The picture is rendered from the source when loading. */
public class PlantUmlImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "plantuml";

    public final String source;
    /** The source from before the change set named by {@link #backupChangeset} changed it, null if the source is unchanged. */
    public final String backupSource;
    public final String backupChangeset;

    @Deprecated public PlantUmlImageEditAreaModel_V002() { // For Jackson only
        source = null;
        backupSource = null;
        backupChangeset = null;
    }

    public PlantUmlImageEditAreaModel_V002(String source, String backupSource, String backupChangeset,
                                           float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.source = source;
        this.backupSource = backupSource;
        this.backupChangeset = backupChangeset;
    }

    public boolean hasBackup() { return backupSource != null; }
}
