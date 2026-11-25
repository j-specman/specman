package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupLineMovingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  /** A, B and the note are in the group which is selected by a rectangle, C is not. */
  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  class C (44444444) at(10, 600)\n"
    + "  note (33333333) at(300, 400) \"n\"\n"
    + "  association 11111111 -> 22222222\n    route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n"
    + "  association 11111111 -> 44444444\n    route 11111111.bottom@60 (80, 250) 44444444.top@60\n"
    + "  anchor 33333333 -- 22222222 route (350, 300)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The waypoints of lines between boxes of the group move with them, those of a line to a box outside stay. */
  @Test
  void linesInsideTheGroupMoveWithIt() throws Exception {
    DiagramEditing editing = groupOf(DIAGRAM);

    drag(editing, 50, 30);

    ClassDiagramModel_V001 model = parsed(editing);
    assertEquals(List.of(70, 250), xsOf(model.relations().get(0).route.waypoints));
    assertEquals(List.of(180, 180), ysOf(model.relations().get(0).route.waypoints));
    assertEquals(List.of(80), xsOf(model.relations().get(1).route.waypoints));
    assertEquals(List.of(250), ysOf(model.relations().get(1).route.waypoints));
    assertEquals(List.of(400), xsOf(model.anchorLines().get(0).waypoints));
    assertEquals(List.of(330), ysOf(model.anchorLines().get(0).waypoints));
  }

  /** The group stops when a waypoint would leave the diagram on the left, so lines and boxes keep their distances. */
  @Test
  void waypointsStopTheGroupAtTheEdge() throws Exception {
    DiagramEditing editing = groupOf(DIAGRAM.replace("(20, 150)", "(3, 150)"));

    drag(editing, -100, 0);

    ClassDiagramModel_V001 model = parsed(editing);
    assertEquals(0, model.relations().get(0).route.waypoints.get(0).x);
    assertEquals(7, classNamed(model, "A").position.x);
    assertEquals(297, classNamed(model, "B").position.x);
  }

  /** Esc during the drag puts the waypoints back too. */
  @Test
  void escapeBringsTheWaypointsBack() throws Exception {
    DiagramEditing editing = groupOf(DIAGRAM);
    Point2D press = centerOf(editing, "A");

    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + 50, press.getY() + 30));
    assertTrue(editing.cancelDrag());
    editing.endDrag();

    ClassDiagramModel_V001 model = parsed(editing);
    assertEquals(List.of(20, 200), xsOf(model.relations().get(0).route.waypoints));
    assertEquals(List.of(350), xsOf(model.anchorLines().get(0).waypoints));
    assertTrue(host.reportedSources.isEmpty());
  }

  private DiagramEditing groupOf(String source) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(source, host);
    Point2D press = new Point2D.Double(0, 0);
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.startRubberBand(press);
    editing.dragTo(new Point2D.Double(700, 520));
    editing.endDrag();
    assertEquals(3, editing.selectedBoxes().size());
    return editing;
  }

  private static void drag(DiagramEditing editing, double dx, double dy) {
    Point2D press = centerOf(editing, "A");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + dx, press.getY() + dy));
    editing.endDrag();
  }

  private static Point2D centerOf(DiagramEditing editing, String className) {
    Rectangle2D area = editing.scene().elementAreas.stream()
      .filter(candidate -> candidate.element instanceof ClassModel_V001 box && box.name.equals(className))
      .findFirst().orElseThrow().area;
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private static ClassDiagramModel_V001 parsed(DiagramEditing editing) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }

  private static ClassModel_V001 classNamed(ClassDiagramModel_V001 model, String name) {
    return model.classes().stream().filter(box -> box.name.equals(name)).findFirst().orElseThrow();
  }

  private static List<Integer> xsOf(List<PositionModel_V001> points) {
    return points.stream().map(point -> point.x).toList();
  }

  private static List<Integer> ysOf(List<PositionModel_V001> points) {
    return points.stream().map(point -> point.y).toList();
  }
}
