package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointInsertionTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n";
  private static final String ROUTED = CLASSES + "  association (33333333) 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}";
  private static final String STRAIGHT = CLASSES + "  association (33333333) 11111111 -- 22222222\n}";
  private static final double TOLERANCE = 4;

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** A press on the selected line puts the waypoint at the closest point of the line, in order. */
  @Test
  void waypointOnTheLine() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    assertTrue(editing.insertWaypointAt(new Point2D.Double(100, 152), TOLERANCE));

    List<PositionModel_V001> waypoints = relationOf(editing.source()).route.waypoints;
    assertEquals(3, waypoints.size());
    assertEquals(100, waypoints.get(1).x);
    assertEquals(150, waypoints.get(1).y);
    assertEquals(20, waypoints.get(0).x);
    assertEquals(200, waypoints.get(2).x);
  }

  /** On the first part of the line, the new waypoint comes first. */
  @Test
  void waypointInFirstPart() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));
    Point2D start = editing.selectedHandles().get(0);
    double middleY = (start.getY() + 150) / 2;

    assertTrue(editing.insertWaypointAt(new Point2D.Double(20, middleY), TOLERANCE));

    List<PositionModel_V001> waypoints = relationOf(editing.source()).route.waypoints;
    assertEquals(3, waypoints.size());
    assertEquals(Math.round(middleY), waypoints.get(0).y);
    assertEquals(150, waypoints.get(1).y);
    assertEquals(20, waypoints.get(1).x);
  }

  /** The new waypoint is shown with a handle, and the relation stays selected. */
  @Test
  void newHandleShown() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.insertWaypointAt(new Point2D.Double(100, 150), TOLERANCE);

    assertEquals(5, editing.selectedHandles().size());
  }

  /** A press away from the line, on a handle, or with nothing selected puts no waypoint. */
  @Test
  void noWaypointElsewhere() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    assertFalse(editing.insertWaypointAt(new Point2D.Double(100, 170), TOLERANCE));
    assertFalse(editing.insertWaypointAt(new Point2D.Double(21, 150), TOLERANCE));
    editing.deselect();
    assertFalse(editing.insertWaypointAt(new Point2D.Double(100, 150), TOLERANCE));
    assertFalse(editing.canUndo());
  }

  /** The first waypoint of a straight relation gives it a route which starts and ends where the line did. */
  @Test
  void routeAlongStraightLine() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(STRAIGHT, host);
    List<Point2D> before = pointsOf(editing);
    editing.selectAt(middleOf(before), TOLERANCE);

    assertTrue(editing.insertWaypointAt(middleOf(before), TOLERANCE));

    assertNotNull(relationOf(editing.source()).route);
    assertEquals(1, relationOf(editing.source()).route.waypoints.size());
    List<Point2D> after = pointsOf(new DiagramEditing(editing.source(), host));
    assertEquals(3, after.size());
    assertNear(before.get(0), after.get(0));
    assertNear(before.get(1), after.get(2));
  }

  /** A loop gets a route over its own points. */
  @Test
  void routeAlongLoop() throws ClassDiagramParseException {
    String loop = "classdiagram {\n  class A (11111111) at(10, 20)\n  association (33333333) 11111111 -- 11111111\n}";
    DiagramEditing editing = new DiagramEditing(loop, host);
    List<Point2D> before = pointsOf(editing);
    editing.selectAt(before.get(1), TOLERANCE);
    Point2D onFirstPart = new Point2D.Double(before.get(0).getX(), (before.get(0).getY() + before.get(1).getY()) / 2);

    assertTrue(editing.insertWaypointAt(onFirstPart, TOLERANCE));

    assertEquals(4, relationOf(editing.source()).route.waypoints.size());
    List<Point2D> after = pointsOf(new DiagramEditing(editing.source(), host));
    assertEquals(6, after.size());
    assertNear(before.get(0), after.get(0));
    assertNear(before.get(4), after.get(5));
  }

  /** Adding a waypoint is one step and no change which a change set tracks. */
  @Test
  void oneUnmarkedStep() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.insertWaypointAt(new Point2D.Double(100, 150), TOLERANCE);

    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
    assertNull(relationOf(editing.source()).change);
  }

  private DiagramEditing selectedAt(String source, Point2D point) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(source, host);
    editing.selectAt(point, TOLERANCE);
    return editing;
  }

  private static List<Point2D> pointsOf(DiagramEditing editing) {
    return editing.scene().relationLines.get(0).points();
  }

  private static Point2D middleOf(List<Point2D> points) {
    return new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
  }

  private static void assertNear(Point2D expected, Point2D actual) {
    assertEquals(expected.getX(), actual.getX(), 0.6);
    assertEquals(expected.getY(), actual.getY(), 0.6);
  }

  private static AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).relations().get(0);
  }
}
