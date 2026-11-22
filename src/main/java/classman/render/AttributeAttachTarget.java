package classman.render;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** An attribute: the line ends at the box around its text, on the side the line comes from, at half the height of the
 * box, or at a point of the border of that box which an anchor fixes. */
public class AttributeAttachTarget extends AttachTarget {
    private final Rectangle2D.Double attributeArea;

    public AttributeAttachTarget(Rectangle2D.Double attributeArea) {
        this.attributeArea = attributeArea;
    }

    @Override
    public Point2D.Double endFacing(Point2D.Double from) {
        boolean fromLeft = from.x < attributeArea.getCenterX();
        return new Point2D.Double(fromLeft ? attributeArea.getMinX() : attributeArea.getMaxX(), attributeArea.getCenterY());
    }

    @Override
    public Double alignmentY() {
        return attributeArea.getCenterY();
    }

    @Override
    public Point2D.Double anchorPoint(Side_V001 side, double offset) {
        return BoxGeometry.sidePoint(attributeArea, side, offset);
    }

    @Override
    public AnchorModel_V001 anchorNear(String id, Point2D point) {
        return BoxGeometry.nearestAnchor(id, attributeArea, point);
    }
}
