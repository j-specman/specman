package classman.render;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** An attribute or method: the line ends at the box around its text, on the side the line comes from, at half the
 * height of the box, or at a point of the border of that box which an anchor fixes. */
public class MemberAttachTarget extends AttachTarget {
    private final Rectangle2D.Double memberArea;

    public MemberAttachTarget(Rectangle2D.Double memberArea) {
        this.memberArea = memberArea;
    }

    @Override
    public Point2D.Double endFacing(Point2D.Double from) {
        boolean fromLeft = from.x < memberArea.getCenterX();
        return new Point2D.Double(fromLeft ? memberArea.getMinX() : memberArea.getMaxX(), memberArea.getCenterY());
    }

    @Override
    public Point2D.Double center() {
        return BoxGeometry.center(memberArea);
    }

    @Override
    public Rectangle2D.Double box() {
        return memberArea;
    }

    @Override
    public Double alignmentY() {
        return memberArea.getCenterY();
    }

    @Override
    public Point2D.Double anchorPoint(Side_V001 side, double offset) {
        return BoxGeometry.sidePoint(memberArea, side, offset);
    }

    @Override
    public AnchorModel_V001 anchorNear(String id, Point2D point) {
        return BoxGeometry.nearestAnchor(id, memberArea, point);
    }
}
