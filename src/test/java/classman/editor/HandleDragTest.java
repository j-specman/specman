package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandleDragTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n";
  private static final String ROUTED = CLASSES + "  association (33333333) 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}";
  private static final String STRAIGHT = CLASSES + "  association (33333333) 11111111 -- 22222222\n}";
  private static final double TOLERANCE = 4;
  private static final double SNAP = 6;

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** A waypoint follows the mouse by the distance it was moved, so it doesn't jump to the mouse. */
  @Test
  void waypointFollowsMouse() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(22, 149), TOLERANCE);
    editing.dragTo(new Point2D.Double(62, 179));
    editing.endDrag();

    PositionModel_V001 moved = relationOf(editing.source()).route.waypoints.get(0);
    assertEquals(60, moved.x);
    assertEquals(180, moved.y);
    assertEquals(200, relationOf(editing.source()).route.waypoints.get(1).x);
  }

  /** The drag is one operation, and the relation stays selected with its handles in the new places. */
  @Test
  void oneStep() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(40, 170));
    editing.dragTo(new Point2D.Double(60, 190));
    assertTrue(host.reportedSources.isEmpty());
    editing.endDrag();

    assertEquals(1, host.reportedSources.size());
    assertEquals(new Point2D.Double(60, 190), editing.selectedHandles().get(1));
  }

  /** A waypoint close to the vertical line through a neighbor snaps onto it. */
  @Test
  void snapsToVerticalLine() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(23, 170), SNAP);
    editing.endDrag();

    assertWaypoint(editing, 0, 20, 170);
  }

  /** A waypoint close to the horizontal line through a neighbor snaps onto it. */
  @Test
  void snapsToHorizontalLine() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(60, 153), SNAP);
    editing.endDrag();

    assertWaypoint(editing, 0, 60, 150);
  }

  /** Close to both lines, the waypoint snaps to the corner of a right angle. */
  @Test
  void snapsToCorner() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(22, 152), SNAP);
    editing.endDrag();

    assertWaypoint(editing, 0, 20, 150);
  }

  /** Farther away than the tolerance, or without a tolerance, nothing snaps. */
  @Test
  void noSnapFarAway() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(32, 170), SNAP);
    assertEquals(new Point2D.Double(32, 170), editing.selectedHandles().get(1));
    editing.dragTo(new Point2D.Double(23, 170));
    assertEquals(new Point2D.Double(23, 170), editing.selectedHandles().get(1));
  }

  /** Between two neighbors which both are close, the closer one wins. */
  @Test
  void snapsToClosestNeighbor() throws ClassDiagramParseException {
    String source = CLASSES + "  association (33333333) 11111111 -- 22222222\n"
      + "  route 11111111.bottom@10 (30, 150) (24, 250) 22222222.bottom@10\n}";
    DiagramEditing editing = selectedAt(source, new Point2D.Double(27, 200));

    editing.startDrag(new Point2D.Double(30, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(23, 150), SNAP);
    editing.endDrag();

    assertEquals(24, waypointOf(editing, 0).x);
  }

  /** The end of a line slides along the border and snaps to the line of its neighbor. */
  @Test
  void endSnapsToo() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));
    Point2D start = editing.selectedHandles().get(0);

    editing.startDrag(start, TOLERANCE);
    editing.dragTo(new Point2D.Double(start.getX() + 4, start.getY()), SNAP);
    editing.endDrag();

    assertEquals(10, relationOf(editing.source()).route.start.offset);
  }

  /** A press on a handle without a move changes nothing. */
  @Test
  void clickOnHandle() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.endDrag();

    assertFalse(editing.canUndo());
  }

  /** The start slides along the side of its class. */
  @Test
  void startSlidesAlongSide() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));
    Point2D start = editing.selectedHandles().get(0);

    editing.startDrag(start, TOLERANCE);
    editing.dragTo(new Point2D.Double(start.getX() + 20, start.getY() + 30));
    editing.endDrag();

    assertEquals(Side_V001.BOTTOM, relationOf(editing.source()).route.start.side);
    assertEquals(30, relationOf(editing.source()).route.start.offset);
  }

  /** An end can be taken around the corner to another side of its class. */
  @Test
  void endMovesToOtherSide() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));
    Rectangle2D classB = areaOfClassAt(editing, 305, 25);
    Point2D end = editing.selectedHandles().get(3);

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(new Point2D.Double(classB.getMaxX() + 60, classB.getMinY() + 15));
    editing.endDrag();

    assertEquals(Side_V001.RIGHT, relationOf(editing.source()).route.end.side);
    assertEquals(15, relationOf(editing.source()).route.end.offset);
  }

  /** Esc during the drag puts the point back, nothing is reported. */
  @Test
  void cancel() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));
    List<Point2D> before = editing.selectedHandles();

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(90, 190));
    boolean cancelled = editing.cancelDrag();

    assertTrue(cancelled);
    assertEquals(before, editing.selectedHandles());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** The first drag of a handle of a relation without a route gives it one, which the line follows. */
  @Test
  void dragMakesRoute() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(STRAIGHT, host);
    List<Point2D> before = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((before.get(0).getX() + before.get(1).getX()) / 2,
      (before.get(0).getY() + before.get(1).getY()) / 2), TOLERANCE);

    editing.startDrag(before.get(1), TOLERANCE);
    editing.dragTo(new Point2D.Double(before.get(1).getX() - 10, before.get(1).getY() + 3));
    editing.endDrag();

    assertEquals(Side_V001.LEFT, relationOf(editing.source()).route.end.side);
    assertEquals(0, relationOf(editing.source()).route.waypoints.size());
    assertEquals(before.get(0).getX(), editing.selectedHandles().get(0).getX(), 0.6);
  }

  /** Cancelling the first drag of a relation without a route takes the route away again. */
  @Test
  void cancelRemovesNewRoute() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(STRAIGHT, host);
    List<Point2D> before = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((before.get(0).getX() + before.get(1).getX()) / 2,
      (before.get(0).getY() + before.get(1).getY()) / 2), TOLERANCE);

    editing.startDrag(before.get(1), TOLERANCE);
    editing.dragTo(new Point2D.Double(before.get(1).getX() - 10, before.get(1).getY() + 3));
    editing.cancelDrag();

    assertEquals(before, editing.selectedHandles());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** A route is no change which a change set tracks. */
  @Test
  void notMarkedWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = selectedAt(ROUTED, new Point2D.Double(100, 150));

    editing.startDrag(new Point2D.Double(20, 150), TOLERANCE);
    editing.dragTo(new Point2D.Double(50, 150));
    editing.endDrag();

    assertNull(relationOf(editing.source()).change);
  }

  private static PositionModel_V001 waypointOf(DiagramEditing editing, int index) throws ClassDiagramParseException {
    return relationOf(editing.source()).route.waypoints.get(index);
  }

  private static void assertWaypoint(DiagramEditing editing, int index, int x, int y)
      throws ClassDiagramParseException {
    assertEquals(x, waypointOf(editing, index).x);
    assertEquals(y, waypointOf(editing, index).y);
  }

  private DiagramEditing selectedAt(String source, Point2D point) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(source, host);
    editing.selectAt(point, TOLERANCE);
    return editing;
  }

  private static Rectangle2D areaOfClassAt(DiagramEditing editing, double x, double y) {
    return editing.scene().elementAreaAt(new Point2D.Double(x, y)).area;
  }

  private static AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).relations().get(0);
  }
}
