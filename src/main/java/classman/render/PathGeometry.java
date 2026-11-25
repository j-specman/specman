package classman.render;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** The indexes of the points where the parts of the path start, the longest part first, parts of equal length in
     * the order of the path. */
    public static List<Integer> segmentsByLength(List<Point2D.Double> path) {
        List<Integer> segments = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            segments.add(i);
        }
        segments.sort(Comparator.comparingDouble((Integer i) -> path.get(i).distance(path.get(i + 1))).reversed());
        return segments;
    }

    /** The y of the line through the two points at the given x. A line which doesn't run sideways is at the y of its first point. */
    public static double yOnLine(Point2D.Double from, Point2D.Double to, double x) {
        double dx = to.x - from.x;
        return dx == 0 ? from.y : from.y + (x - from.x) * (to.y - from.y) / dx;
    }

    /** The x of the line through the two points at the given y. A line which doesn't run up or down is at the x of its first point. */
    public static double xOnLine(Point2D.Double from, Point2D.Double to, double y) {
        double dy = to.y - from.y;
        return dy == 0 ? from.x : from.x + (y - from.y) * (to.x - from.x) / dy;
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

    /** True if a text of the given size stands better above or below the part from one point to the other than right
     * or left of it: if it has to be moved less up or down to clear a slanted line than sideways. A flat line is
     * sideways, a steep one not, and a wide text takes a steeper line as sideways than a narrow one. */
    public static boolean runsSideways(Point2D.Double from, Point2D.Double to, double textWidth, double textHeight) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        return textWidth * dy * dy <= textHeight * dx * dx;
    }

    /** The point which lies the given distance along the path from its start, the distance limited to the path. */
    public static Point2D.Double pointAtDistance(List<? extends Point2D> path, double distance) {
        double wanted = Math.max(0, Math.min(length(path), distance));
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
        return new Point2D.Double(path.get(0).getX(), path.get(0).getY());
    }

    /** The middle of a text which stands the given distance along the path from its start and the given distance
     * across it, left of the line if that is positive, seen in the direction of the path. */
    public static Point2D.Double pointBeside(List<? extends Point2D> path, double distance, double across) {
        Point2D.Double onLine = pointAtDistance(path, distance);
        Point2D.Double direction = directionAtDistance(path, distance);
        return new Point2D.Double(onLine.x + direction.y * across, onLine.y - direction.x * across);
    }

    /** The unit vector in which the path runs at the given distance from its start, in the part which the distance
     * falls into, in the last one for a distance beyond the path. A path without length runs to the right. */
    public static Point2D.Double directionAtDistance(List<? extends Point2D> path, double distance) {
        double wanted = Math.max(0, distance);
        double passed = 0;
        Point2D.Double last = new Point2D.Double(1, 0);
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D from = path.get(i);
            Point2D to = path.get(i + 1);
            double segment = from.distance(to);
            if (segment == 0) {
                continue;
            }
            last = new Point2D.Double((to.getX() - from.getX()) / segment, (to.getY() - from.getY()) / segment);
            if (passed + segment >= wanted) {
                return last;
            }
            passed += segment;
        }
        return last;
    }

    /** Where the point stands in relation to the path: the distance along it of its closest point, and the distance
     * from the line, left of the line if it is positive, seen in the direction of the path. */
    public static PathSpot spotNear(List<? extends Point2D> path, Point2D point) {
        double closestDistance = Double.MAX_VALUE;
        PathSpot spot = new PathSpot(0, 0);
        double passed = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D from = path.get(i);
            Point2D to = path.get(i + 1);
            double segment = from.distance(to);
            if (segment == 0) {
                continue;
            }
            double part = projection(from, to, point, segment);
            double nearX = from.getX() + part * (to.getX() - from.getX());
            double nearY = from.getY() + part * (to.getY() - from.getY());
            double distance = Math.hypot(point.getX() - nearX, point.getY() - nearY);
            if (distance < closestDistance) {
                closestDistance = distance;
                double leftX = (to.getY() - from.getY()) / segment;
                double leftY = -(to.getX() - from.getX()) / segment;
                spot = new PathSpot(passed + part * segment,
                    (point.getX() - nearX) * leftX + (point.getY() - nearY) * leftY);
            }
            passed += segment;
        }
        return spot;
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

    /** The length of the path. */
    public static double length(List<? extends Point2D> path) {
        double length = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            length += path.get(i).distance(path.get(i + 1));
        }
        return length;
    }
}
