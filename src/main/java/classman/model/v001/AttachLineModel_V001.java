package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/**
 * Model of a dashed line which attaches a box to something else: the anchor line of a note, or the association class
 * link of a class. It is an element of the diagram like a relation, so a change set can add it or remove it.
 */
public abstract class AttachLineModel_V001 extends AbstractDiagramElementModel_V001 {
    /** The ID of the box the line starts at. */
    public String startId;
    /** The ID of the element the line leads to. */
    public String targetId;
    /** Absolute points the line leads over, empty for a straight line. */
    public final List<PositionModel_V001> waypoints = new ArrayList<>();
    /** Where the line starts on the border of its box, null if the line leaves the box where it faces its first
     * waypoint or its target. Its class ID is {@link #startId}. */
    public AnchorModel_V001 start;
    /** Where the line ends on the target, null if it is computed. Its class ID is {@link #targetId}. */
    public AnchorModel_V001 end;

    protected AttachLineModel_V001(String startId, String targetId) {
        this.startId = startId;
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
