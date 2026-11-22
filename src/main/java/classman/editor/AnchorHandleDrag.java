package classman.editor;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The dragging of one handle of the anchor of a note: a waypoint goes where the mouse takes it, the start slides along
 * the border of the note, the end slides along the border of the class the note is attached to, or is hung onto another
 * class while the mouse is on it. The point follows the mouse by the distance between where the handle was grabbed and
 * its center, so it doesn't jump, and snaps to the line of its neighbors if it comes close. Cancelling takes the
 * waypoints, the ends and the target back. */
class AnchorHandleDrag extends Drag {
  private final RouteEditing routeEditing;
  private final NoteModel_V001 note;
  private final Rectangle2D noteArea;
  private final int handle;
  private final List<Point2D> path;
  private final Point2D pressedAt;
  private final Map<String, Rectangle2D> reachableClasses;
  private final String ownClassId;
  private final List<PositionModel_V001> waypointsBefore;
  private final AnchorModel_V001 startBefore;
  private final AnchorModel_V001 endBefore;
  private String hungOntoClassId;
  private boolean moved;

  /** @param path the points of the anchor line as it is drawn at the start: the start, the waypoints, the end
   * @param handle the index of the dragged point in the path: 0 is the start, the last the end
   * @param reachableClasses the classes, by ID, which the end may be hung onto, in the order of drawing: all of them,
   * or only the one the note is attached to if the anchor can't be hung onto another class now */
  AnchorHandleDrag(RouteEditing routeEditing, NoteModel_V001 note, Rectangle2D noteArea, int handle, List<Point2D> path,
                   Point2D pressedAt, Map<String, Rectangle2D> reachableClasses) {
    this.routeEditing = routeEditing;
    this.note = note;
    this.noteArea = noteArea;
    this.handle = handle;
    this.path = path;
    this.pressedAt = pressedAt;
    this.reachableClasses = reachableClasses;
    this.ownClassId = note.attachedToId;
    this.hungOntoClassId = ownClassId;
    this.waypointsBefore = new ArrayList<>(note.attachWaypoints);
    this.startBefore = note.attachStart;
    this.endBefore = note.attachEnd;
  }

  @Override
  boolean hasMoved() { return moved; }

  /** @param tolerance how close the point has to come to the line of a neighbor to snap to it, and how close the
   * mouse has to come to another class to hang the end onto it */
  @Override
  void to(Point2D at, double tolerance) {
    moved = true;
    Point2D grabbed = path.get(handle);
    Point2D target = PointSnapper.snapped(new Point2D.Double(grabbed.getX() + at.getX() - pressedAt.getX(),
      grabbed.getY() + at.getY() - pressedAt.getY()), neighbors(), tolerance);
    if (handle == 0) {
      routeEditing.attachNoteStart(note, noteArea, target);
    }
    else if (handle == path.size() - 1) {
      hungOntoClassId = classAt(at, tolerance);
      routeEditing.attachNoteEnd(note, hungOntoClassId, reachableClasses.get(hungOntoClassId), target);
    }
    else {
      routeEditing.moveWaypoint(note, handle - 1, target);
    }
  }

  /** @return where the class is which the dragged end is hung onto now, if that is not the class the note was
   * attached to when the drag started, null otherwise */
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
    note.attachWaypoints.clear();
    note.attachWaypoints.addAll(waypointsBefore);
    note.attachStart = startBefore;
    note.attachEnd = endBefore;
    note.attachedToId = ownClassId;
    hungOntoClassId = ownClassId;
    moved = false;
  }
}
