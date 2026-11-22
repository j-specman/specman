package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.Side_V001;
import classman.render.BoxGeometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Changes the route of a relation, and the waypoints of the anchor of a note. A route is no change which a change
 * set tracks, so nothing here is marked. */
class RouteEditing {

  /** Removes a waypoint. With the last one the route is gone, and the relation runs as if it never had one.
   * @param waypointIndex the index in the waypoints of the route */
  void deleteWaypoint(AbstractRelationModel_V001 relation, int waypointIndex) {
    relation.route.waypoints.remove(waypointIndex);
    if (relation.route.waypoints.isEmpty()) {
      relation.route = null;
    }
  }

  /** Puts a new waypoint into the part of the line which starts at the given point of its path. A relation without a
   * route gets one first, see {@link #ensureRoute}.
   * @param path the points of the line as it is drawn now
   * @param segment the index of the point in the path where the part starts
   * @param fromArea where the source class is, toArea where the target class is */
  void insertWaypoint(AbstractRelationModel_V001 relation, List<Point2D> path, int segment, Point2D at,
                      Rectangle2D fromArea, Rectangle2D toArea) {
    ensureRoute(relation, path, fromArea, toArea);
    relation.route.waypoints.add(segment, positionOf(at));
  }

  /** Gives a relation without a route one which runs along the path it has now: its ends become anchors where the
   * line now touches the classes, and the inner points of the path become waypoints. */
  void ensureRoute(AbstractRelationModel_V001 relation, List<Point2D> path, Rectangle2D fromArea,
                   Rectangle2D toArea) {
    if (relation.route == null) {
      relation.route = routeAlong(relation, path, fromArea, toArea);
    }
  }

  /** Removes a waypoint of the anchor of a note. Without waypoints the anchor is a straight line. */
  void deleteWaypoint(NoteModel_V001 note, int waypointIndex) {
    note.attachWaypoints.remove(waypointIndex);
  }

  /** Puts a new waypoint into the anchor of a note, as the waypoint with the given index. */
  void insertWaypoint(NoteModel_V001 note, int waypointIndex, Point2D at) {
    note.attachWaypoints.add(waypointIndex, positionOf(at));
  }

  void moveWaypoint(NoteModel_V001 note, int waypointIndex, Point2D to) {
    note.attachWaypoints.set(waypointIndex, positionOf(to));
  }

  /** Fixes the start of the anchor of a note at the point of the border of the note which is closest to the given
   * point. The note needs an ID. */
  void attachNoteStart(NoteModel_V001 note, Rectangle2D noteArea, Point2D to) {
    note.attachStart = anchorAt(note.id, noteArea, to);
  }

  /** Fixes the end of the anchor of a note at the point of the border of the class which is closest to the given
   * point. The note is attached to that class from then on. */
  void attachNoteEnd(NoteModel_V001 note, String classId, Rectangle2D classArea, Point2D to) {
    note.attachedToId = classId;
    note.attachEnd = anchorAt(classId, classArea, to);
  }

  /** Takes the route of the anchor of a note away: the waypoints and the fixed ends. */
  void clearRoute(NoteModel_V001 note) {
    note.attachWaypoints.clear();
    note.attachStart = null;
    note.attachEnd = null;
  }

  /** @param waypointIndex the index in the waypoints of the route */
  void moveWaypoint(AbstractRelationModel_V001 relation, int waypointIndex, Point2D to) {
    relation.route.waypoints.set(waypointIndex, positionOf(to));
  }

  /** Hangs the start of the route onto the class, at the point of its border which is closest to the given point. It
   * is the source class of the relation from then on. */
  void attachStart(AbstractRelationModel_V001 relation, String classId, Rectangle2D classArea, Point2D to) {
    RouteModel_V001 route = relation.route;
    relation.fromId = classId;
    relation.route = new RouteModel_V001(anchorAt(classId, classArea, to), route.waypoints, route.end);
  }

  /** Hangs the end of the route onto the class, at the point of its border which is closest to the given point. It is
   * the target class of the relation from then on. */
  void attachEnd(AbstractRelationModel_V001 relation, String classId, Rectangle2D classArea, Point2D to) {
    RouteModel_V001 route = relation.route;
    relation.toId = classId;
    relation.route = new RouteModel_V001(route.start, route.waypoints, anchorAt(classId, classArea, to));
  }

  /** @return a copy of the route which doesn't change when the route does, null if there is none */
  RouteModel_V001 copyOf(RouteModel_V001 route) {
    return route != null ? new RouteModel_V001(route.start, route.waypoints, route.end) : null;
  }

  private RouteModel_V001 routeAlong(AbstractRelationModel_V001 relation, List<Point2D> path, Rectangle2D fromArea,
                                     Rectangle2D toArea) {
    List<PositionModel_V001> waypoints = new ArrayList<>();
    for (Point2D point : path.subList(1, path.size() - 1)) {
      waypoints.add(positionOf(point));
    }
    return new RouteModel_V001(anchorAt(relation.fromId, fromArea, path.get(0)), waypoints,
      anchorAt(relation.toId, toArea, path.get(path.size() - 1)));
  }

  private static PositionModel_V001 positionOf(Point2D point) {
    return new PositionModel_V001((int) Math.round(point.getX()), (int) Math.round(point.getY()));
  }

  /** The anchor at the point of the border of the class which is closest to the given point. */
  private AnchorModel_V001 anchorAt(String classId, Rectangle2D area, Point2D point) {
    AnchorModel_V001 closest = null;
    double closestDistance = Double.MAX_VALUE;
    for (Side_V001 side : Side_V001.values()) {
      int offset = offsetOn(side, area, point);
      double distance = BoxGeometry.sidePoint(area, side, offset).distance(point);
      if (distance < closestDistance) {
        closest = new AnchorModel_V001(classId, side, offset);
        closestDistance = distance;
      }
    }
    return closest;
  }

  /** How far from the start of the side the point is, in whole pixels, at most the length of the side. */
  private static int offsetOn(Side_V001 side, Rectangle2D area, Point2D point) {
    boolean horizontal = side == Side_V001.TOP || side == Side_V001.BOTTOM;
    double along = horizontal ? point.getX() - area.getMinX() : point.getY() - area.getMinY();
    double length = horizontal ? area.getWidth() : area.getHeight();
    return (int) Math.round(Math.max(0, Math.min(length, along)));
  }
}
