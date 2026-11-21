package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.RouteModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The dragging of one handle of a relation: a waypoint goes where the mouse takes it, an end slides along the border
 * of its class. The point follows the mouse by the distance between where the handle was grabbed and its center, so
 * it doesn't jump. It snaps to the line of its neighbors if it comes close, see {@link #snapped}, so that vertical and
 * horizontal lines are easy to get. An end is hung onto another class while the mouse is on that class or close to it,
 * and stays on the class it caught last while the mouse is on none. A relation without a route gets one on the first
 * move. Cancelling takes the route, the classes and the points back. */
class HandleDrag extends Drag {
  private final RouteEditing routeEditing;
  private final AbstractRelationModel_V001 relation;
  private final List<Point2D> path;
  private final int handle;
  private final Point2D pressedAt;
  private final Rectangle2D fromArea;
  private final Rectangle2D toArea;
  private final Map<String, Rectangle2D> reachableClasses;
  private final String ownClassId;
  private boolean moved;
  private RouteModel_V001 routeBefore;
  private String fromIdBefore;
  private String toIdBefore;
  private String hungOntoClassId;

  /** @param path the points of the line as it is drawn at the start, the handles are these points
   * @param handle the index of the dragged point in the path: 0 is the start, the last the end
   * @param fromArea where the source class is, toArea where the target class is
   * @param reachableClasses the classes, by ID, which the dragged end may be hung onto, in the order of drawing: all of
   * them, or only its own if the relation can't be hung onto another class now */
  HandleDrag(RouteEditing routeEditing, AbstractRelationModel_V001 relation, List<Point2D> path, int handle,
             Point2D pressedAt, Rectangle2D fromArea, Rectangle2D toArea, Map<String, Rectangle2D> reachableClasses) {
    this.routeEditing = routeEditing;
    this.relation = relation;
    this.path = path;
    this.handle = handle;
    this.pressedAt = pressedAt;
    this.fromArea = fromArea;
    this.toArea = toArea;
    this.reachableClasses = reachableClasses;
    this.ownClassId = handle == 0 ? relation.fromId : relation.toId;
    this.hungOntoClassId = ownClassId;
  }

  @Override
  boolean hasMoved() { return moved; }

  /** @param tolerance how close the point has to come to the line of a neighbor to snap to it, and how close the
   * mouse has to come to another class to hang the end onto it */
  @Override
  void to(Point2D at, double tolerance) {
    if (!moved) {
      rememberState();
      routeEditing.ensureRoute(relation, path, fromArea, toArea);
      moved = true;
    }
    Point2D grabbed = path.get(handle);
    Point2D target = snapped(new Point2D.Double(grabbed.getX() + at.getX() - pressedAt.getX(),
      grabbed.getY() + at.getY() - pressedAt.getY()), tolerance);
    if (handle == 0) {
      hungOntoClassId = classAt(at, tolerance);
      routeEditing.attachStart(relation, hungOntoClassId, reachableClasses.get(hungOntoClassId), target);
    }
    else if (handle == path.size() - 1) {
      hungOntoClassId = classAt(at, tolerance);
      routeEditing.attachEnd(relation, hungOntoClassId, reachableClasses.get(hungOntoClassId), target);
    }
    else {
      routeEditing.moveWaypoint(relation, handle - 1, target);
    }
  }

  /** @return where the class is which the dragged end is hung onto now, if that is not the class it was hung onto when
   * the drag started, null otherwise */
  @Override
  Rectangle2D connectionTarget() {
    boolean otherClass = !hungOntoClassId.equals(ownClassId);
    return otherClass ? reachableClasses.get(hungOntoClassId) : null;
  }

  private void rememberState() {
    routeBefore = routeEditing.copyOf(relation.route);
    fromIdBefore = relation.fromId;
    toIdBefore = relation.toId;
  }

  /** @return the ID of the class which is closest to the point, if the point is on it or at most the tolerance away
   * from it, the topmost one if several are equally close, otherwise the ID of the class the end hangs on now, so the
   * end keeps the class it caught last and doesn't jump back while the mouse is in the open */
  private String classAt(Point2D point, double tolerance) {
    String closest = null;
    double closestDistance = tolerance;
    for (Map.Entry<String, Rectangle2D> reachable : reachableClasses.entrySet()) {
      double distance = distance(reachable.getValue(), point);
      if (distance <= closestDistance) {
        closest = reachable.getKey();
        closestDistance = distance;
      }
    }
    return closest != null ? closest : hungOntoClassId;
  }

  /** The distance from the point to the rectangle, 0 if the point is on it. */
  private static double distance(Rectangle2D area, Point2D point) {
    double dx = Math.max(Math.max(area.getMinX() - point.getX(), 0), point.getX() - area.getMaxX());
    double dy = Math.max(Math.max(area.getMinY() - point.getY(), 0), point.getY() - area.getMaxY());
    return Math.hypot(dx, dy);
  }

  /** A coordinate which is within the tolerance of the same coordinate of a neighbor in the line becomes that one, so
   * that the part of the line to that neighbor is vertical or horizontal. Each coordinate takes the closest neighbor
   * within the tolerance, so a point can snap to one neighbor in x and to the other in y. */
  private Point2D snapped(Point2D target, double tolerance) {
    List<Double> neighborXs = new ArrayList<>();
    List<Double> neighborYs = new ArrayList<>();
    for (Point2D neighbor : neighbors()) {
      neighborXs.add(neighbor.getX());
      neighborYs.add(neighbor.getY());
    }
    return new Point2D.Double(snapped(target.getX(), neighborXs, tolerance),
      snapped(target.getY(), neighborYs, tolerance));
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

  /** The points before and after the dragged one in the line. They stay where they were while it is dragged. */
  private List<Point2D> neighbors() {
    List<Point2D> neighbors = new ArrayList<>();
    if (handle > 0) {
      neighbors.add(path.get(handle - 1));
    }
    if (handle < path.size() - 1) {
      neighbors.add(path.get(handle + 1));
    }
    return neighbors;
  }

  @Override
  void cancel() {
    if (moved) {
      relation.route = routeBefore;
      relation.fromId = fromIdBefore;
      relation.toId = toIdBefore;
      hungOntoClassId = ownClassId;
      moved = false;
    }
  }
}
