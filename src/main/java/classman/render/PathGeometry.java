package classman.render;

import java.awt.geom.Point2D;
import java.util.List;

/** Questions about the path of a relation. */
public final class PathGeometry {

    private PathGeometry() {
    }

    /** The index of the point where the longest part of the path starts. */
    public static int longestSegment(List<Point2D.Double> path) {
        int longest = 0;
        double longestLength = -1;
        for (int i = 0; i < path.size() - 1; i++) {
            double length = path.get(i).distance(path.get(i + 1));
            if (length > longestLength) {
                longest = i;
                longestLength = length;
            }
        }
        return longest;
    }

    public static Point2D.Double middle(Point2D.Double from, Point2D.Double to) {
        return new Point2D.Double((from.x + to.x) / 2, (from.y + to.y) / 2);
    }

    /** The point where things are attached to a relation: the middle of the longest part of its path. */
    public static Point2D.Double attachPoint(List<Point2D.Double> path) {
        int longest = longestSegment(path);
        return middle(path.get(longest), path.get(longest + 1));
    }

    /** The point a quarter of the way along the longest part of the path, where the cross of a removed relation stands. */
    public static Point2D.Double quarterPoint(List<Point2D.Double> path) {
        int longest = longestSegment(path);
        Point2D.Double from = path.get(longest);
        Point2D.Double to = path.get(longest + 1);
        return new Point2D.Double(from.x + (to.x - from.x) / 4, from.y + (to.y - from.y) / 4);
    }

    /** True if the part from one point to the other runs more sideways than up or down. */
    public static boolean isHorizontal(Point2D.Double from, Point2D.Double to) {
        return Math.abs(to.x - from.x) > Math.abs(to.y - from.y);
    }

    /** The point which lies the given percent of the length of the path from its start, the percent limited to 0 to 100. */
    public static Point2D.Double pointAlong(List<? extends Point2D> path, double percent) {
        double wanted = Math.max(0, Math.min(100, percent)) / 100 * length(path);
        double passed = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D from = path.get(i);
            Point2D to = path.get(i + 1);
            double segment = from.distance(to);
            if (passed + segment >= wanted && segment > 0) {
                double part = (wanted - passed) / segment;
                return new Point2D.Double(from.getX() + part * (to.getX() - from.getX()),
                    from.getY() + part * (to.getY() - from.getY()));
            }
            passed += segment;
        }
        Point2D start = path.get(0);
        return new Point2D.Double(start.getX(), start.getY());
    }

    /** How many percent of the length of the path lie before the point of the path which is closest to the given point. */
    public static double percentNear(List<? extends Point2D> path, Point2D point) {
        double total = length(path);
        double closestDistance = Double.MAX_VALUE;
        double closestAlong = 0;
        double passed = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D from = path.get(i);
            Point2D to = path.get(i + 1);
            double segment = from.distance(to);
            double part = segment == 0 ? 0 : projection(from, to, point, segment);
            double distance = new Point2D.Double(from.getX() + part * (to.getX() - from.getX()),
                from.getY() + part * (to.getY() - from.getY())).distance(point);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestAlong = passed + part * segment;
            }
            passed += segment;
        }
        return total == 0 ? 0 : closestAlong / total * 100;
    }

    /** Where on the part from one point to the other, as a fraction of its length limited to 0 to 1, the given point
     * is closest. */
    private static double projection(Point2D from, Point2D to, Point2D point, double segment) {
        double along = ((point.getX() - from.getX()) * (to.getX() - from.getX())
            + (point.getY() - from.getY()) * (to.getY() - from.getY())) / (segment * segment);
        return Math.max(0, Math.min(1, along));
    }

    private static double length(List<? extends Point2D> path) {
        double length = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            length += path.get(i).distance(path.get(i + 1));
        }
        return length;
    }
}
