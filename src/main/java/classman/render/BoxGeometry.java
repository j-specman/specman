package classman.render;

import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Points on and around the rectangle of a box. */
public final class BoxGeometry {

    private BoxGeometry() {
    }

    public static Point2D.Double center(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getCenterY());
    }

    /** The point where the line from the center of the box to the target leaves the box. */
    public static Point2D.Double exitPoint(Rectangle2D box, Point2D target) {
        Point2D.Double center = center(box);
        double dx = target.getX() - center.x;
        double dy = target.getY() - center.y;
        if (dx == 0 && dy == 0) {
            return center;
        }
        double toVerticalSide = dx == 0 ? Double.POSITIVE_INFINITY : box.getWidth() / 2 / Math.abs(dx);
        double toHorizontalSide = dy == 0 ? Double.POSITIVE_INFINITY : box.getHeight() / 2 / Math.abs(dy);
        double scale = Math.min(toVerticalSide, toHorizontalSide);
        return new Point2D.Double(center.x + dx * scale, center.y + dy * scale);
    }

    /** The point on a side of the box, the offset counted from the start of the side and limited to the side. */
    public static Point2D.Double sidePoint(Rectangle2D box, Side_V001 side, double offset) {
        switch (side) {
            case TOP:
                return new Point2D.Double(box.getX() + clamp(offset, box.getWidth()), box.getY());
            case BOTTOM:
                return new Point2D.Double(box.getX() + clamp(offset, box.getWidth()), box.getMaxY());
            case LEFT:
                return new Point2D.Double(box.getX(), box.getY() + clamp(offset, box.getHeight()));
            default:
                return new Point2D.Double(box.getMaxX(), box.getY() + clamp(offset, box.getHeight()));
        }
    }

    /** The smallest rectangle which covers both. Either may be null, then the result is the other one. */
    public static Rectangle2D.Double union(Rectangle2D.Double first, Rectangle2D.Double second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        Rectangle2D.Double result = new Rectangle2D.Double();
        Rectangle2D.union(first, second, result);
        return result;
    }

    private static double clamp(double offset, double sideLength) {
        return Math.max(0, Math.min(offset, sideLength));
    }
}
