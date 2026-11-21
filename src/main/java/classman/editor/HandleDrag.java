package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.RouteModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** The dragging of one handle of a relation: a waypoint goes where the mouse takes it, an end slides along the border
 * of its class. The point follows the mouse by the distance between where the handle was grabbed and its center, so
 * it doesn't jump. It snaps to the line of its neighbors if it comes close, see {@link #snapped}, so that vertical and
 * horizontal lines are easy to get. A relation without a route gets one on the first move, which is taken away again
 * if the drag is cancelled. */
class HandleDrag extends Drag {
  private final RouteEditing routeEditing;
  private final AbstractRelationModel_V001 relation;
  private final List<Point2D> path;
  private final int handle;
  private final Point2D pressedAt;
  private final Rectangle2D fromArea;
  private final Rectangle2D toArea;
  private boolean moved;
  private RouteModel_V001 routeBefore;

  /** @param path the points of the line as it is drawn at the start, the handles are these points
   * @param handle the index of the dragged point in the path: 0 is the start, the last the end
   * @param fromArea where the source class is, toArea where the target class is */
  HandleDrag(RouteEditing routeEditing, AbstractRelationModel_V001 relation, List<Point2D> path, int handle,
             Point2D pressedAt, Rectangle2D fromArea, Rectangle2D toArea) {
    this.routeEditing = routeEditing;
    this.relation = relation;
    this.path = path;
    this.handle = handle;
    this.pressedAt = pressedAt;
    this.fromArea = fromArea;
    this.toArea = toArea;
  }

  @Override
  boolean hasMoved() { return moved; }

  @Override
  void to(Point2D at, double snapTolerance) {
    if (!moved) {
      routeBefore = routeEditing.copyOf(relation.route);
      routeEditing.ensureRoute(relation, path, fromArea, toArea);
      moved = true;
    }
    Point2D grabbed = path.get(handle);
    Point2D target = snapped(new Point2D.Double(grabbed.getX() + at.getX() - pressedAt.getX(),
      grabbed.getY() + at.getY() - pressedAt.getY()), snapTolerance);
    if (handle == 0) {
      routeEditing.moveStart(relation, fromArea, target);
    }
    else if (handle == path.size() - 1) {
      routeEditing.moveEnd(relation, toArea, target);
    }
    else {
      routeEditing.moveWaypoint(relation, handle - 1, target);
    }
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
      moved = false;
    }
  }
}
