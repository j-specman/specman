package specman.model.v002;

import specman.ChangeInfo;

/** A PlantUML diagram, persisted as its source code. The picture is rendered from the source when loading.
 * Changes of the source are tracked as a whole: A change of the source in change mode keeps the source from before
 * as backup, together with the name of the change set which changed it. */
public class PlantUmlImageEditAreaModel_V002 extends AbstractSourceImageEditAreaModel_V002 {
    public static final String IMAGE_TYPE = "plantuml";

    /** The source from before the change set named by {@link #backupChangeset} changed it, null if the source is unchanged. */
    public final String backupSource;
    public final String backupChangeset;

    @Deprecated public PlantUmlImageEditAreaModel_V002() { // For Jackson only
        backupSource = null;
        backupChangeset = null;
    }

    public PlantUmlImageEditAreaModel_V002(String source, String backupSource, String backupChangeset,
                                           float individualScalePercent, ChangeInfo changeInfo) {
        super(source, individualScalePercent, changeInfo);
        this.backupSource = backupSource;
        this.backupChangeset = backupChangeset;
    }

    public boolean hasBackup() { return backupSource != null; }
}
