package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a note, the box with the folded corner. */
public class NoteModel_V001 extends AbstractDiagramElementModel_V001 {
    /** Line breaks are real newline characters, not the two characters \n. */
    public String text;
    /** Null if the source gives no position, the layout places the note then. */
    public PositionModel_V001 position;
    /** Null if the content decides about the width. */
    public Integer width;
    /** The ID of the class, attribute or relation the note is attached to with a dashed line, null if it isn't attached. */
    public String attachedToId;
    /** Absolute points the dashed line leads over, empty for a straight line. Only used if the note is attached. */
    public List<PositionModel_V001> attachWaypoints = new ArrayList<>();

    public NoteModel_V001(String text) {
        this.text = text;
    }
}
