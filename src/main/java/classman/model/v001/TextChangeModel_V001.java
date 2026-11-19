package classman.model.v001;

/** The mark which says that a change set changed a text. The text itself is the new one. */
public class TextChangeModel_V001 {
    /** The name of the change set, which the host application maps to a color. */
    public final String changeSet;
    /** The text before the change. Empty if the text was empty, null if there was no text before. */
    public final String backup;

    public TextChangeModel_V001(String changeSet, String backup) {
        this.changeSet = changeSet;
        this.backup = backup;
    }
}
