package specman.model.v002;

import specman.ChangeInfo;

/** An image area which shows a diagram rendered from its source, which is persisted as text. A change of the source in
 * change mode keeps the source from before as backup, together with the name of the change set which changed it. */
public abstract class AbstractSourceImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public final String source;
    /** The source from before the change set named by {@link #backupChangeset} changed it, null if the source is unchanged. */
    public final String backupSource;
    public final String backupChangeset;

    protected AbstractSourceImageEditAreaModel_V002() { // For Jackson only
        source = null;
        backupSource = null;
        backupChangeset = null;
    }

    protected AbstractSourceImageEditAreaModel_V002(String source, String backupSource, String backupChangeset,
                                                    float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.source = source;
        this.backupSource = backupSource;
        this.backupChangeset = backupChangeset;
    }

    public boolean hasBackup() { return backupSource != null; }
}
