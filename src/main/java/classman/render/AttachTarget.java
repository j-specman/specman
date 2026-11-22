package classman.render;

import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;

/** Something a note can be attached to: where the dashed line to it ends. */
public abstract class AttachTarget {

    /** The point on the target where the line ends which comes from the given point, the note or its last waypoint. */
    public abstract Point2D.Double endFacing(Point2D.Double from);

    /** @return the point on the border of the target which is a given distance from the start of a side, null if the
     * target has no sides to fix a line to */
    public Point2D.Double anchorPoint(Side_V001 side, double offset) {
        return null;
    }
}
