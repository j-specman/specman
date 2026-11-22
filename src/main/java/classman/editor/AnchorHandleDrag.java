package classman.editor;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.AttachTarget;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The dragging of one handle of an anchor line: a waypoint goes where the mouse takes it, the start slides along
 * the border of the note, the end slides along the border of the class the line leads to, or is hung onto another
 * class while the mouse is on it. If the line leads to an attribute or a relation, the end slides along the border
 * of the attribute or the line of the relation and stays on it. The point follows the mouse by the distance between
 * where the handle was grabbed and its center, so it doesn't jump, and snaps to the line of its neighbors if it comes
 * close. Cancelling takes the waypoints, the ends and the target back. */
class AnchorHandleDrag extends Drag {
  private final RouteEditing routeEditing;
  private final AnchorLineModel_V001 anchorLine;
  private final Rectangle2D noteArea;
  private final int handle;
  private final List<Point2D> path;
  private final Point2D pressedAt;
  private final Map<String, Rectangle2D> reachableClasses;
  private final AttachTarget slideTarget;
  private final AttachTarget ownTarget;
  private final boolean startIsComputed;
  private final boolean endIsComputed;
  private final String ownClassId;
  private final List<PositionModel_V001> waypointsBefore;
  private final AnchorModel_V001 startBefore;
  private final AnchorModel_V001 endBefore;
  private String hungOntoClassId;
  private boolean moved;

  /** @param path the points of the anchor line as it is drawn at the start: the start, the waypoints, the end
   * @param handle the index of the dragged point in the path: 0 is the start, the last the end
   * @param reachableClasses the classes, by ID, which the end may be hung onto, in the order of drawing: all of them,
   * or only the one the line leads to if it can't be hung onto another class now
   * @param slideTarget the attribute or relation the line leads to, whose border or line the end slides along
   * without leaving it, null if the line leads to a class
   * @param ownTarget what the line leads to when the drag starts */
  AnchorHandleDrag(RouteEditing routeEditing, AnchorLineModel_V001 anchorLine, Rectangle2D noteArea, int handle, List<Point2D> path,
                   Point2D pressedAt, Map<String, Rectangle2D> reachableClasses, AttachTarget slideTarget,
                   AttachTarget ownTarget) {
    this.routeEditing = routeEditing;
    this.anchorLine = anchorLine;
    this.noteArea = noteArea;
    this.handle = handle;
    this.path = path;
    this.pressedAt = pressedAt;
    this.reachableClasses = reachableClasses;
    this.slideTarget = slideTarget;
    this.ownTarget = ownTarget;
    this.startIsComputed = anchorLine.start == null;
    this.endIsComputed = anchorLine.end == null;
    this.ownClassId = anchorLine.targetId;
    this.hungOntoClassId = ownClassId;
    this.waypointsBefore = new ArrayList<>(anchorLine.waypoints);
    this.startBefore = anchorLine.start;
    this.endBefore = anchorLine.end;
  }

  @Override
  boolean hasMoved() { return moved; }

  /** @param tolerance how close the point has to come to the line of a neighbor to snap to it, and how close the
   * mouse has to come to another class to hang the end onto it */
  @Override
  void to(Point2D at, double tolerance) {
    moved = true;
    Point2D grabbed = path.get(handle);
    List<Double> neighborXs = new ArrayList<>();
    List<Double> neighborYs = new ArrayList<>();
    collectSnapCoordinates(neighborXs, neighborYs);
    Point2D target = PointSnapper.snapped(new Point2D.Double(grabbed.getX() + at.getX() - pressedAt.getX(),
      grabbed.getY() + at.getY() - pressedAt.getY()), neighborXs, neighborYs, tolerance);
    if (handle == 0) {
      routeEditing.attachNoteStart(anchorLine, noteArea, target);
    }
    else if (handle == path.size() - 1 && slideTarget != null) {
      anchorLine.end = slideTarget.anchorNear(ownClassId, target);
    }
    else if (handle == path.size() - 1) {
      hungOntoClassId = classAt(at, tolerance);
      routeEditing.attachNoteEnd(anchorLine, hungOntoClassId, reachableClasses.get(hungOntoClassId), target);
    }
    else {
      routeEditing.moveWaypoint(anchorLine, handle - 1, target);
    }
  }

  /** @return where the class is which the dragged end is hung onto now, if that is not the class the line
   * led to when the drag started, null otherwise */
  @Override
  Rectangle2D connectionTarget() {
    return hungOntoClassId.equals(ownClassId) ? null : reachableClasses.get(hungOntoClassId);
  }

  /** @return the ID of the class which is closest to the point, if the point is on it or at most the tolerance away
   * from it, otherwise the ID of the class the end hangs on now, so the end keeps the class it caught last */
  private String classAt(Point2D point, double tolerance) {
    String closest = ClassPicker.closest(reachableClasses, point, tolerance);
    return closest != null ? closest : hungOntoClassId;
  }

  /** The coordinates which the dragged point snaps to: those of the points before and after it in the line, which stay
   * where they were while it is dragged. An end of the line which isn't fixed doesn't stay: it moves with the dragged
   * point, so the point it was at when the drag started is nothing to snap to. What counts for such an end is where the
   * point has to be so that the line leaves the note, or meets the target, straight: the center of the note or of the
   * class, the middle of the height of an attribute. */
  private void collectSnapCoordinates(List<Double> xs, List<Double> ys) {
    if (handle > 0) {
      addSnapCoordinates(handle - 1, xs, ys);
    }
    if (handle < path.size() - 1) {
      addSnapCoordinates(handle + 1, xs, ys);
    }
  }

  private void addSnapCoordinates(int index, List<Double> xs, List<Double> ys) {
    Double x = path.get(index).getX();
    Double y = path.get(index).getY();
    if (index == 0 && startIsComputed) {
      x = noteArea.getCenterX();
      y = noteArea.getCenterY();
    }
    else if (index == path.size() - 1 && endIsComputed) {
      x = ownTarget.alignmentX();
      y = ownTarget.alignmentY();
    }
    if (x != null) {
      xs.add(x);
    }
    if (y != null) {
      ys.add(y);
    }
  }

  @Override
  void cancel() {
    anchorLine.waypoints.clear();
    anchorLine.waypoints.addAll(waypointsBefore);
    anchorLine.start = startBefore;
    anchorLine.end = endBefore;
    anchorLine.targetId = ownClassId;
    hungOntoClassId = ownClassId;
    moved = false;
  }
}
