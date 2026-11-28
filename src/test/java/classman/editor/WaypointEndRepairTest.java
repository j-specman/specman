package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaypointEndRepairTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  /** The line leaves the bottom of A, runs to the waypoint at (100, 150), and from the one at (200, 150) to the bottom of B. */
  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (100, 150) (200, 150) 22222222.bottom@10\n}";
  private static final Point2D FIRST_WAYPOINT = new Point2D.Double(100, 150);
  private static final Point2D SECOND_WAYPOINT = new Point2D.Double(200, 150);

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The waypoint is moved above A: the line would run through A, so its start is fixed where the line enters. */
  @Test
  void startMovesToWhereTheLineEntersTheBox() throws ClassDiagramParseException {
    DiagramEditing editing = draggingFrom(FIRST_WAYPOINT);

    editing.dragTo(new Point2D.Double(30, 5));
    editing.endDrag();

    assertEquals(Side_V001.TOP, routeOf(editing).start.side);
  }

  /** The same goes for the end, next to the last waypoint. */
  @Test
  void endMovesToWhereTheLineEntersTheBox() throws ClassDiagramParseException {
    DiagramEditing editing = draggingFrom(SECOND_WAYPOINT);

    editing.dragTo(new Point2D.Double(340, 5));
    editing.endDrag();

    assertEquals(Side_V001.TOP, routeOf(editing).end.side);
  }

  /** Moving the waypoint back brings the start back to where the user had fixed it. */
  @Test
  void movingBackRestoresTheStart() throws ClassDiagramParseException {
    DiagramEditing editing = draggingFrom(FIRST_WAYPOINT);

    editing.dragTo(new Point2D.Double(30, 5));
    editing.dragTo(new Point2D.Double(100, 170));
    editing.endDrag();

    assertEquals(Side_V001.BOTTOM, routeOf(editing).start.side);
    assertEquals(10, routeOf(editing).start.offset);
  }

  /** Esc puts the start back too. */
  @Test
  void escapeRestoresTheStart() throws ClassDiagramParseException {
    DiagramEditing editing = draggingFrom(FIRST_WAYPOINT);
    editing.dragTo(new Point2D.Double(30, 5));

    editing.cancelDrag();
    editing.endDrag();

    assertEquals(Side_V001.BOTTOM, routeOf(editing).start.side);
  }

  /** A waypoint which leaves the line leaving its box properly keeps the ends. */
  @Test
  void ordinaryMoveChangesNothing() throws ClassDiagramParseException {
    DiagramEditing editing = draggingFrom(FIRST_WAYPOINT);

    editing.dragTo(new Point2D.Double(100, 170));
    editing.endDrag();

    assertEquals(Side_V001.BOTTOM, routeOf(editing).start.side);
    assertEquals(10, routeOf(editing).start.offset);
    assertEquals(Side_V001.BOTTOM, routeOf(editing).end.side);
  }

  /** The line of a note is repaired like a relation. */
  @Test
  void anchorLineIsRepairedToo() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (33333333) at(10, 20) \"eins\"\n  class zwei (22222222) at(300, 20)\n"
      + "  anchor 33333333 -- 22222222 route 33333333.bottom@10 (100, 150) 22222222.bottom@10\n}";
    DiagramEditing editing = new DiagramEditing(source, host);
    editing.selectAt(new Point2D.Double(100, 150), TOLERANCE);

    editing.startDrag(FIRST_WAYPOINT, TOLERANCE);
    editing.dragTo(new Point2D.Double(30, 5));
    editing.endDrag();

    assertEquals(Side_V001.TOP, parse(editing).anchorLines().get(0).start.side);
  }

  /** A loop has no route before the first move, which makes one: dragging its waypoints must work, and Esc takes the
   * route away again. */
  @Test
  void waypointOfLoopWithoutRouteCanBeDragged() throws ClassDiagramParseException {
    String loop = "classdiagram {\n  class A (11111111) at(10, 20)\n  association (33333333) 11111111 -- 11111111\n}";
    DiagramEditing editing = new DiagramEditing(loop, host);
    java.util.List<Point2D> points = editing.scene().relationLines.get(0).points();
    editing.selectAt(points.get(1), TOLERANCE);

    editing.startDrag(points.get(1), TOLERANCE);
    editing.dragTo(new Point2D.Double(points.get(1).getX() + 30, points.get(1).getY() + 30));
    editing.cancelDrag();
    editing.endDrag();

    assertNull(routeOf(editing));
  }

  private DiagramEditing draggingFrom(Point2D waypoint) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAt(new Point2D.Double(100, 150), TOLERANCE);
    editing.startDrag(waypoint, TOLERANCE);
    return editing;
  }

  private static classman.model.v001.RouteModel_V001 routeOf(DiagramEditing editing) throws ClassDiagramParseException {
    AbstractRelationModel_V001 relation = parse(editing).relations().get(0);
    return relation.route;
  }

  private static classman.model.v001.ClassDiagramModel_V001 parse(DiagramEditing editing)
      throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }
}
