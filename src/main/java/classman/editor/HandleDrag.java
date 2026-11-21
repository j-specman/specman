package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.RouteModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** The dragging of one handle of a relation: a waypoint goes where the mouse takes it, an end slides along the border
 * of its class. The point follows the mouse by the distance between where the handle was grabbed and its center, so
 * it doesn't jump. A relation without a route gets one on the first move, which is taken away again if the drag is
 * cancelled. */
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
  void to(Point2D at) {
    if (!moved) {
      routeBefore = routeEditing.copyOf(relation.route);
      routeEditing.ensureRoute(relation, path, fromArea, toArea);
      moved = true;
    }
    Point2D grabbed = path.get(handle);
    Point2D target = new Point2D.Double(grabbed.getX() + at.getX() - pressedAt.getX(),
      grabbed.getY() + at.getY() - pressedAt.getY());
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

  @Override
  void cancel() {
    if (moved) {
      relation.route = routeBefore;
      moved = false;
    }
  }
}
