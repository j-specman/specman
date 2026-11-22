package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/**
 * Model of the dashed line from a note to the class, attribute or relation it is about, the anchor of the note. It is
 * an element of the diagram like a relation, so a change set can add it or remove it. A note can have several.
 */
public class AnchorLineModel_V001 extends AbstractDiagramElementModel_V001 {
    /** The ID of the note the line starts at. */
    public String noteId;
    /** The ID of the class, attribute or relation the line leads to. */
    public String targetId;
    /** Absolute points the line leads over, empty for a straight line. */
    public final List<PositionModel_V001> waypoints = new ArrayList<>();
    /** Where the line starts on the border of the note, null if the line leaves the note where it faces its first
     * waypoint or its target. Its class ID is {@link #noteId}. */
    public AnchorModel_V001 start;
    /** Where the line ends on the target, null if it is computed. Its class ID is {@link #targetId}. */
    public AnchorModel_V001 end;

    public AnchorLineModel_V001(String noteId, String targetId) {
        this.noteId = noteId;
        this.targetId = targetId;
    }

    /** @return true if a change set has marked the line as removed */
    public boolean isRemoved() {
        return change != null && change.type == ChangeType_V001.REMOVED;
    }

    /** Takes the waypoints and the fixed ends away, so the line is the straight one which the layout works out. */
    public void clearRoute() {
        waypoints.clear();
        start = null;
        end = null;
    }
}
