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
}
