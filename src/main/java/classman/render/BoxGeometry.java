package classman.render;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Points on and around the rectangle of a box. */
public final class BoxGeometry {
    private static final double INTERIOR_EPSILON = 1e-6;

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

    /** The anchor on the side of the box which is closest to the point, at the point of that side which is closest to
     * it, the offset in whole pixels. */
    public static AnchorModel_V001 nearestAnchor(String boxId, Rectangle2D box, Point2D point) {
        AnchorModel_V001 closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Side_V001 side : Side_V001.values()) {
            int offset = offsetOn(side, box, point);
            double distance = sidePoint(box, side, offset).distance(point);
            if (distance < closestDistance) {
                closest = new AnchorModel_V001(boxId, side, offset);
                closestDistance = distance;
            }
        }
        return closest;
    }

    /** How far from the start of the side the point is, in whole pixels, at most the length of the side. Only the
     * coordinate along the side counts. */
    public static int offsetOn(Side_V001 side, Rectangle2D box, Point2D point) {
        boolean horizontal = side == Side_V001.TOP || side == Side_V001.BOTTOM;
        double along = horizontal ? point.getX() - box.getMinX() : point.getY() - box.getMinY();
        double length = horizontal ? box.getWidth() : box.getHeight();
        return (int) Math.round(clamp(along, length));
    }

    /**
     * Where a line from a point outside of the box to a point on its border first enters the interior of the box, if
     * it runs through the box before it gets to the end. The line only touching the border at its end, running along
     * the border, or starting inside of the box doesn't count: null then.
     */
    public static Point2D.Double entryIntoInterior(Rectangle2D box, Point2D from, Point2D to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double[] directions = {-dx, dx, -dy, dy};
        double[] distances = {from.getX() - box.getMinX(), box.getMaxX() - from.getX(),
            from.getY() - box.getMinY(), box.getMaxY() - from.getY()};
        double enter = 0;
        double exit = 1;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i] == 0) {
                if (distances[i] < 0) {
                    return null;
                }
                continue;
            }
            double t = distances[i] / directions[i];
            if (directions[i] < 0) {
                enter = Math.max(enter, t);
            } else {
                exit = Math.min(exit, t);
            }
        }
        if (enter <= INTERIOR_EPSILON || exit - enter <= INTERIOR_EPSILON) {
            return null;
        }
        double middle = (enter + exit) / 2;
        double middleX = from.getX() + dx * middle;
        double middleY = from.getY() + dy * middle;
        boolean middleIsInside = middleX > box.getMinX() + INTERIOR_EPSILON && middleX < box.getMaxX() - INTERIOR_EPSILON
            && middleY > box.getMinY() + INTERIOR_EPSILON && middleY < box.getMaxY() - INTERIOR_EPSILON;
        return middleIsInside ? new Point2D.Double(from.getX() + dx * enter, from.getY() + dy * enter) : null;
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
