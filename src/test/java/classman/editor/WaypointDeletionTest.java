package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointDeletionTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String ROUTED = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}";
  private static final double TOLERANCE = 4;

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final DiagramEditing editing;

  WaypointDeletionTest() throws ClassDiagramParseException {
    editing = new DiagramEditing(ROUTED, host, new EditorTexts(host.locale()));
    editing.selectAt(new Point2D.Double(100, 150), TOLERANCE);
  }

  /** A double click on a waypoint removes it, the others stay in their order. */
  @Test
  void waypointRemoved() throws ClassDiagramParseException {
    boolean deleted = editing.deleteWaypointAt(new Point2D.Double(21, 149), TOLERANCE);

    assertTrue(deleted);
    var waypoints = relationOf(editing.source()).route.waypoints;
    assertEquals(1, waypoints.size());
    assertEquals(200, waypoints.get(0).x);
    assertEquals(150, waypoints.get(0).y);
  }

  /** The relation stays selected, and the handle is gone. */
  @Test
  void relationStaysSelected() {
    editing.deleteWaypointAt(new Point2D.Double(20, 150), TOLERANCE);

    assertEquals(3, editing.selectedHandles().size());
  }

  /** Without its last waypoint a relation has no route any more. */
  @Test
  void routeGoneWithLastWaypoint() throws ClassDiagramParseException {
    editing.deleteWaypointAt(new Point2D.Double(20, 150), TOLERANCE);
    assertNotNull(relationOf(editing.source()).route);

    editing.deleteWaypointAt(new Point2D.Double(200, 150), TOLERANCE);

    assertNull(relationOf(editing.source()).route);
    assertEquals(2, editing.selectedHandles().size());
  }

  /** The handles at the ends are no waypoints. */
  @Test
  void endsStay() throws ClassDiagramParseException {
    Point2D start = editing.selectedHandles().get(0);
    Point2D end = editing.selectedHandles().get(3);

    assertFalse(editing.deleteWaypointAt(start, TOLERANCE));
    assertFalse(editing.deleteWaypointAt(end, TOLERANCE));
    assertFalse(editing.canUndo());
  }

  /** A point on the line which is no waypoint deletes nothing. */
  @Test
  void pointOnTheLineKept() {
    assertFalse(editing.deleteWaypointAt(new Point2D.Double(100, 150), TOLERANCE));
  }

  /** The deletion is one undo step. */
  @Test
  void oneStep() {
    editing.deleteWaypointAt(new Point2D.Double(20, 150), TOLERANCE);

    assertTrue(editing.canUndo());
    assertEquals(1, host.reportedSources.size());
  }

  /** The route is no change: without a mark also while changes are tracked. */
  @Test
  void notMarkedWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";

    editing.deleteWaypointAt(new Point2D.Double(20, 150), TOLERANCE);

    assertNull(relationOf(editing.source()).change);
  }

  /** Without a route there is no waypoint, also if the relation has points of its own, like a loop. */
  @Test
  void relationWithoutRoute() throws ClassDiagramParseException {
    DiagramEditing loop = new DiagramEditing("classdiagram {\n  class A (11111111) at(10, 20)\n"
      + "  association (33333333) 11111111 -- 11111111\n}", host, new EditorTexts(host.locale()));
    loop.selectAt(loop.scene().relationLines.get(0).points().get(1), TOLERANCE);

    assertFalse(loop.deleteWaypointAt(loop.selectedHandles().get(1), TOLERANCE));
  }

  /** A press on a handle of the selected relation keeps it selected, also where a box lies below the handle. */
  @Test
  void pressOnHandleKeepsRelation() throws ClassDiagramParseException {
    Point2D start = editing.selectedHandles().get(0);
    editing.selectAt(new Point2D.Double(start.getX(), start.getY() - 1), TOLERANCE);

    assertEquals(4, editing.selectedHandles().size());
    assertNull(editing.selectedArea());
  }

  private static AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    ClassDiagramModel_V001 model = new ClassDiagramParser_V001().parse(source);
    return model.relations().get(0);
  }
}
