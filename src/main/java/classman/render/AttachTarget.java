package classman.render;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Something a note can be attached to: where the dashed line to it ends. */
public abstract class AttachTarget {

    /** The point on the target where the line ends which comes from the given point, the note or its last waypoint. */
    public abstract Point2D.Double endFacing(Point2D.Double from);

    /** @return the middle of the target: for a relation the point where a line to it ends */
    public abstract Point2D.Double center();

    /** @return the area of a class or attribute, null for a relation */
    public Rectangle2D.Double box() {
        return null;
    }

    /** @return the point on the border of the target which is a given distance from the start of a side, null if the
     * target has no sides to fix a line to */
    public Point2D.Double anchorPoint(Side_V001 side, double offset) {
        return null;
    }

    /** @return the x which a waypoint needs for the line from it to the target to run vertically, as long as the end
     * on the target isn't fixed, null if the end doesn't depend on the waypoint in x or the target has no such x */
    public Double alignmentX() {
        return null;
    }

    /** @return the y which a waypoint needs for the line from it to the target to run horizontally, as long as the end
     * on the target isn't fixed, null if there is none */
    public Double alignmentY() {
        return null;
    }

    /** @return the point on the line of the target which is a given percent of its length from its start, null if the
     * target has no line */
    public Point2D.Double pointAlong(double percent) {
        return null;
    }

    /** @return the point where the anchor fixes the end of the line, null if the anchor doesn't fit the target */
    public Point2D.Double fixedPoint(AnchorModel_V001 anchor) {
        return anchor.isOnLine() ? pointAlong(anchor.offset) : anchorPoint(anchor.side, anchor.offset);
    }

    /** @param id the ID of the target
     * @return the anchor at the point of the target which is closest to the given point, null if the target has no
     * point to fix a line to */
    public AnchorModel_V001 anchorNear(String id, Point2D point) {
        return null;
    }
}
