package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ends at a box which slide along it so that a horizontal or vertical part of the line stays straight. */
class StraightEndFollowingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(180, 150)\n  association (33333333) 11111111 -- 22222222\n";
  /** From the right side of A at y=30 to (200, 30), then down to the top side of B at x=200. */
  private static final String L_ROUTE = CLASSES + "  route 11111111.right@10 (200, 30) 22222222.top@20\n}";
  /** The first part is slanted, the second one vertical. */
  private static final String SLANTED_AND_VERTICAL = CLASSES
    + "  route 11111111.right@10 (200, 100) 22222222.top@20\n}";
  private static final String TWO_WAYPOINTS = CLASSES
    + "  route 11111111.right@10 (200, 30) (200, 100) 22222222.top@20\n}";
  private static final String STRAIGHT = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n}";
  private static final double TOLERANCE = 4;

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** Dragging the corner of an L moves both ends along their sides, so both parts stay straight. */
  @Test
  void bothEndsOfLRoute() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(L_ROUTE, new Point2D.Double(200, 100));

    drag(editing, 1, new Point2D.Double(220, 35));

    assertEquals(Side_V001.RIGHT, routeOf(editing).start.side);
    assertEquals(15, routeOf(editing).start.offset);
    assertEquals(Side_V001.TOP, routeOf(editing).end.side);
    assertEquals(40, routeOf(editing).end.offset);
  }

  /** Both ends move with one drag, which is one operation. */
  @Test
  void oneOperation() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(L_ROUTE, new Point2D.Double(200, 100));

    drag(editing, 1, new Point2D.Double(220, 35));

    assertEquals(1, host.reportedSources.size());
  }

  /** The end stops at the end of its side, and the part gets slanted. */
  @Test
  void stopsAtEndOfSide() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(L_ROUTE, new Point2D.Double(200, 100));
    Rectangle2D classB = areaOfClassAt(editing, 185, 155);

    drag(editing, 1, new Point2D.Double(5000, 30));

    assertEquals((int) Math.round(classB.getWidth()), routeOf(editing).end.offset);
    assertEquals(10, routeOf(editing).start.offset);
  }

  /** A part which is slanted when the drag starts doesn't get its end moved. */
  @Test
  void slantedPartStaysAsItIs() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(SLANTED_AND_VERTICAL, new Point2D.Double(200, 125));

    drag(editing, 1, new Point2D.Double(220, 110));

    assertEquals(10, routeOf(editing).start.offset);
    assertEquals(40, routeOf(editing).end.offset);
  }

  /** A straight part between two waypoints isn't kept straight, only parts which end at a box. */
  @Test
  void waypointsAreNotMoved() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(TWO_WAYPOINTS, new Point2D.Double(200, 125));

    drag(editing, 2, new Point2D.Double(220, 100));

    assertEquals(10, routeOf(editing).start.offset);
    assertEquals(200, routeOf(editing).waypoints.get(0).x);
    assertEquals(30, routeOf(editing).waypoints.get(0).y);
    assertEquals(40, routeOf(editing).end.offset);
  }

  /** The dragged end of a straight line between two boxes takes the other end along. */
  @Test
  void straightLineBetweenBoxes() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(STRAIGHT, host);
    List<Point2D> before = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((before.get(0).getX() + before.get(1).getX()) / 2, before.get(0).getY()),
      TOLERANCE);

    drag(editing, 0, new Point2D.Double(before.get(0).getX(), before.get(0).getY() + 5));

    assertEquals(Side_V001.RIGHT, routeOf(editing).start.side);
    assertEquals(Side_V001.LEFT, routeOf(editing).end.side);
    assertEquals(routeOf(editing).start.offset, routeOf(editing).end.offset);
    assertEquals(before.get(1).getY() + 5, editing.selectedHandles().get(1).getY(), 0.6);
  }

  /** Esc puts the ends back where they were. */
  @Test
  void cancelPutsEndsBack() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(L_ROUTE, new Point2D.Double(200, 100));
    List<Point2D> before = editing.selectedHandles();

    editing.startDrag(before.get(1), TOLERANCE);
    editing.dragTo(new Point2D.Double(220, 35));
    editing.cancelDrag();

    assertEquals(before, editing.selectedHandles());
    assertTrue(host.reportedSources.isEmpty());
  }

  private void drag(DiagramEditing editing, int handle, Point2D to) {
    editing.startDrag(editing.selectedHandles().get(handle), TOLERANCE);
    editing.dragTo(to);
    editing.endDrag();
  }

  private DiagramEditing selectedAt(String source, Point2D point) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(source, host);
    editing.selectAt(point, TOLERANCE);
    return editing;
  }

  private static Rectangle2D areaOfClassAt(DiagramEditing editing, double x, double y) {
    return editing.scene().elementAreaAt(new Point2D.Double(x, y)).area;
  }

  private static RouteModel_V001 routeOf(DiagramEditing editing)
      throws ClassDiagramParseException {
    AbstractRelationModel_V001 relation = new ClassDiagramParser_V001().parse(editing.source()).relations().get(0);
    return relation.route;
  }
}
