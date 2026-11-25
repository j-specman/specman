package classman.editor;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** Lets a point which is dragged snap to the lines of its neighbors, so that vertical and horizontal lines are easy
 * to get. */
final class PointSnapper {

  private PointSnapper() {}

  /** A coordinate which is within the tolerance of the same coordinate of a neighbor becomes that one, so that the part
   * of the line to that neighbor is vertical or horizontal. Each coordinate takes the closest neighbor within the
   * tolerance, so a point can snap to one neighbor in x and to the other in y. */
  static Point2D snapped(Point2D target, List<Point2D> neighbors, double tolerance) {
    List<Double> neighborXs = new ArrayList<>();
    List<Double> neighborYs = new ArrayList<>();
    for (Point2D neighbor : neighbors) {
      neighborXs.add(neighbor.getX());
      neighborYs.add(neighbor.getY());
    }
    return snapped(target, neighborXs, neighborYs, tolerance);
  }

  /** Like {@link #snapped(Point2D, List, double)}, for neighbors which have only a x or only a y worth snapping to.
   * @param xs the x coordinates a x can snap to
   * @param ys the y coordinates a y can snap to */
  static Point2D snapped(Point2D target, List<Double> xs, List<Double> ys, double tolerance) {
    return new Point2D.Double(snapped(target.getX(), xs, tolerance), snapped(target.getY(), ys, tolerance));
  }

  private static double snapped(double value, List<Double> candidates, double tolerance) {
    double snapped = value;
    double closestDistance = tolerance;
    for (double candidate : candidates) {
      double distance = Math.abs(candidate - value);
      if (distance <= closestDistance) {
        snapped = candidate;
        closestDistance = distance;
      }
    }
    return snapped;
  }
}
