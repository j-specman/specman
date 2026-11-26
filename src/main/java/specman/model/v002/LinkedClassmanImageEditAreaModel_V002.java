package specman.model.v002;

import specman.ChangeInfo;

/** A Classman class diagram which lives in a file of its own, persisted as the link to that file, a Classman URL. The
 * diagram is read from the file when loading. */
public class LinkedClassmanImageEditAreaModel_V002 extends AbstractImageEditAreaModel_V002 {
    public final String link;

    @Deprecated public LinkedClassmanImageEditAreaModel_V002() { // For Jackson only
        link = null;
    }

    public LinkedClassmanImageEditAreaModel_V002(String link, float individualScalePercent, ChangeInfo changeInfo) {
        super(individualScalePercent, changeInfo);
        this.link = link;
    }
}
