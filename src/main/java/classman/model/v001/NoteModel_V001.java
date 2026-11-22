package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a note, the box with the folded corner. */
public class NoteModel_V001 extends AbstractBoxModel_V001 {
    /** Line breaks are real newline characters, not the two characters \n. */
    public String text;
    /** Null if no change set changed the text. */
    public TextChangeModel_V001 textChange;
    /** The ID of the class, attribute or relation the note is attached to with a dashed line, null if it isn't attached. */
    public String attachedToId;
    /** Absolute points the dashed line leads over, empty for a straight line. Only used if the note is attached. */
    public List<PositionModel_V001> attachWaypoints = new ArrayList<>();

    public NoteModel_V001(String text) {
        this.text = text;
    }
}
