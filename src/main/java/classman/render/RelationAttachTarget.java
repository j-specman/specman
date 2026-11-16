package classman.render;

import java.awt.geom.Point2D;

/** A relation: the line ends at the middle of the longest part of the relation. */
public class RelationAttachTarget extends AttachTarget {
    private final Point2D.Double attachPoint;

    public RelationAttachTarget(Point2D.Double attachPoint) {
        this.attachPoint = attachPoint;
    }

    @Override
    public Point2D.Double endFacing(Point2D.Double from) {
        return attachPoint;
    }
}
