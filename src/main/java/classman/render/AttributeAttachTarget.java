package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** An attribute: the line ends at the box around its text, on the side the line comes from, at half the height of the box. */
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
}
