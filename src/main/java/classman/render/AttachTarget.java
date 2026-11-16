package classman.render;

import java.awt.geom.Point2D;

/** Something a note can be attached to: where the dashed line to it ends. */
public abstract class AttachTarget {

    /** The point on the target where the line ends which comes from the given point, the note or its last waypoint. */
    public abstract Point2D.Double endFacing(Point2D.Double from);
}
