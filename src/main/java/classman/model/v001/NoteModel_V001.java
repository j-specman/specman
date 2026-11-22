package classman.model.v001;

/** Model of a note, the box with the folded corner. The dashed lines from it to what it is about are anchor lines. */
public class NoteModel_V001 extends AbstractBoxModel_V001 {
    /** Line breaks are real newline characters, not the two characters \n. */
    public String text;
    /** Null if no change set changed the text. */
    public TextChangeModel_V001 textChange;

    public NoteModel_V001(String text) {
        this.text = text;
    }
}
