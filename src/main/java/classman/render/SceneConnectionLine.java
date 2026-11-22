package classman.render;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** A line which connects boxes of the diagram and which the user can select: the line of a relation, or the anchor of
 * a note. It knows its points and how far a point is from it. */
public abstract class SceneConnectionLine {
    public final ScenePolyline line;
    /** Marks at the ends of the line, like a diamond or an arrowhead, which are drawn above the line. */
    public final List<AbstractSceneElement> decorations;

    protected SceneConnectionLine(ScenePolyline line, List<AbstractSceneElement> decorations) {
        this.line = line;
        this.decorations = List.copyOf(decorations);
    }

    /** @return the same line and marks, all drawn with another stroke width and with round corners, so that the sharp
     * tips of the marks don't poke far into the box they point at */
    public abstract SceneConnectionLine withStrokeWidth(double width);

    /** @return the points of the line: where it starts, its waypoints, where it ends */
    public List<Point2D> points() {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < line.xs.length; i++) {
            points.add(new Point2D.Double(line.xs[i], line.ys[i]));
        }
        return points;
    }

    /** @return the distance of the point to the closest part of the line */
    public double distanceTo(Point2D point) {
        return distanceToSegment(nearestSegment(point), point);
    }

    /** @return the index of the point where the part of the line starts which is closest to the given point */
    public int nearestSegment(Point2D point) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < line.xs.length - 1; i++) {
            double distance = distanceToSegment(i, point);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** @return the point on the part of the line which starts at the given index which is closest to the point */
    public Point2D nearestPointOnSegment(int segment, Point2D point) {
        double fromX = line.xs[segment];
        double fromY = line.ys[segment];
        double dx = line.xs[segment + 1] - fromX;
        double dy = line.ys[segment + 1] - fromY;
        double lengthSquared = dx * dx + dy * dy;
        double along = lengthSquared == 0 ? 0
            : ((point.getX() - fromX) * dx + (point.getY() - fromY) * dy) / lengthSquared;
        double clamped = Math.max(0, Math.min(1, along));
        return new Point2D.Double(fromX + clamped * dx, fromY + clamped * dy);
    }

    private double distanceToSegment(int segment, Point2D point) {
        return Line2D.ptSegDist(line.xs[segment], line.ys[segment], line.xs[segment + 1], line.ys[segment + 1],
            point.getX(), point.getY());
    }
}
