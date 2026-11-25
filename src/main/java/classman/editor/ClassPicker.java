package classman.editor;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

/** Finds the class which the mouse is on or close to, for the things which are connected to classes. */
final class ClassPicker {

  private ClassPicker() {}

  /** @param classes the areas of the classes by key, in the order of drawing
   * @return the key of the class which is closest to the point, if the point is on it or at most the tolerance away
   * from it, the topmost one if several are equally close, null if there is none */
  static <K> K closest(Map<K, Rectangle2D> classes, Point2D point, double tolerance) {
    K closest = null;
    double closestDistance = tolerance;
    for (Map.Entry<K, Rectangle2D> candidate : classes.entrySet()) {
      double distance = distance(candidate.getValue(), point);
      if (distance <= closestDistance) {
        closest = candidate.getKey();
        closestDistance = distance;
      }
    }
    return closest;
  }

  /** The distance from the point to the rectangle, 0 if the point is on it. */
  private static double distance(Rectangle2D area, Point2D point) {
    double dx = Math.max(Math.max(area.getMinX() - point.getX(), 0), point.getX() - area.getMaxX());
    double dy = Math.max(Math.max(area.getMinY() - point.getY(), 0), point.getY() - area.getMaxY());
    return Math.hypot(dx, dy);
  }
}
