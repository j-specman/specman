package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineEndRepairTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  /** The line leaves the bottom of "eins" and ends at the left side of "zwei", like in the picture of the idea. */
  private static final String DIAGRAM = "classdiagram {\n  class eins (11111111) at(10, 20)\n"
    + "  class zwei (22222222) at(300, 200)\n"
    + "  association 11111111 -> 22222222\n    route 11111111.bottom@30 22222222.left@10\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The line would run through "zwei" after "eins" has moved to the right: its end is fixed where the line enters. */
  @Test
  void endMovesToWhereTheLineEntersTheBox() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);

    dragEins(editing, 490, 0);

    Rectangle2D zwei = areaOf(editing, "zwei");
    assertEquals(zwei.getMinY(), lastPointOfLine(editing).getY(), 0.5);
    assertTrue(lastPointOfLine(editing).getX() > zwei.getMinX() && lastPointOfLine(editing).getX() < zwei.getMaxX());
  }

  /** The new place stays after the mouse is released. */
  @Test
  void repairedEndIsPartOfTheOperation() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);

    dragEinsAndRelease(editing, 490, 0);

    assertEquals(1, host.reportedSources.size());
    ClassDiagramModel_V001 model = new ClassDiagramParser_V001().parse(editing.source());
    assertEquals(Side_V001.TOP, model.relations().get(0).route.end.side);
  }

  /** Moving the box back brings the end back to where the user had fixed it. */
  @Test
  void movingBackRestoresTheEnd() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    Point2D press = centerOf(editing, "eins");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);

    editing.dragTo(new Point2D.Double(press.getX() + 490, press.getY()));
    assertEquals(areaOf(editing, "zwei").getMinY(), lastPointOfLine(editing).getY(), 0.5);
    editing.dragTo(new Point2D.Double(press.getX() + 5, press.getY()));

    assertEquals(areaOf(editing, "zwei").getMinX(), lastPointOfLine(editing).getX(), 0.5);
    assertEquals(areaOf(editing, "zwei").getMinY() + 10, lastPointOfLine(editing).getY(), 0.5);
  }

  /** Esc puts the ends back too. */
  @Test
  void escapeRestoresTheEnd() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    Point2D press = centerOf(editing, "eins");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + 490, press.getY()));
    assertEquals(areaOf(editing, "zwei").getMinY(), lastPointOfLine(editing).getY(), 0.5);

    assertTrue(editing.cancelDrag());
    editing.endDrag();

    assertEquals(Side_V001.LEFT, routeOf(editing).end.side);
    assertEquals(10, routeOf(editing).end.offset);
  }

  /** A line which still leaves its boxes properly keeps its ends. */
  @Test
  void ordinaryMoveChangesNothing() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);

    dragEinsAndRelease(editing, 0, 10);

    assertEquals(Side_V001.LEFT, routeOf(editing).end.side);
    assertEquals(10, routeOf(editing).end.offset);
    assertEquals(30, routeOf(editing).start.offset);
  }

  /** The start would run through its own box when the other box is moved above: it is fixed where the line leaves. */
  @Test
  void startOnTheMovedBoxItselfIsRepaired() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    Point2D press = centerOf(editing, "zwei");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);

    editing.dragTo(new Point2D.Double(press.getX(), press.getY() - 190));

    Rectangle2D eins = areaOf(editing, "eins");
    assertEquals(eins.getMaxX(), firstPointOfLine(editing).getX(), 0.5);
  }

  private static Point2D lastPointOfLine(DiagramEditing editing) {
    java.util.List<Point2D> points = editing.scene().relationLines.get(0).points();
    return points.get(points.size() - 1);
  }

  private static Point2D firstPointOfLine(DiagramEditing editing) {
    return editing.scene().relationLines.get(0).points().get(0);
  }

  /** Lines between boxes which are moved together move rigidly: nothing is changed. */
  @Test
  void groupMovesRigidLinesAsTheyAre() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAllBoxes();
    Point2D press = centerOf(editing, "eins");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);

    editing.dragTo(new Point2D.Double(press.getX() + 300, press.getY() + 50));
    editing.endDrag();

    assertEquals(Side_V001.LEFT, routeOf(editing).end.side);
    assertEquals(Side_V001.BOTTOM, routeOf(editing).start.side);
  }

  /** The line of a note is repaired like a relation. */
  @Test
  void anchorLineIsRepairedToo() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (33333333) at(10, 20) \"eins\"\n  class zwei (22222222) at(300, 200)\n"
      + "  anchor 33333333 -- 22222222 route 33333333.bottom@10 22222222.left@10\n}";
    DiagramEditing editing = new DiagramEditing(source, host);
    Rectangle2D note = areaOf(editing, "eins");
    Point2D press = new Point2D.Double(note.getCenterX(), note.getCenterY());
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);

    editing.dragTo(new Point2D.Double(press.getX() + 490, press.getY()));
    editing.endDrag();

    ClassDiagramModel_V001 model = new ClassDiagramParser_V001().parse(editing.source());
    assertEquals(Side_V001.TOP, model.anchorLines().get(0).end.side);
  }

  /** The line goes down from "eins" and turns at a waypoint to the left side of "zwei". */
  private static final String WITH_WAYPOINT = "classdiagram {\n  class eins (11111111) at(10, 20)\n"
    + "  class zwei (22222222) at(300, 200)\n"
    + "  association 11111111 -> 22222222\n    route 11111111.bottom@30 (40, 210) 22222222.left@10\n}";

  /** A box which is moved over a waypoint of a line which ends at it eats the waypoint, and the end is fixed where the
   * line then enters the box, so the line doesn't run through it. */
  @Test
  void movedBoxEatsTheWaypointItCovers() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(WITH_WAYPOINT, host);
    assertEquals(3, editing.scene().relationLines.get(0).points().size());

    dragZwei(editing, -280, 0);

    assertEquals(2, editing.scene().relationLines.get(0).points().size());
    assertEquals(areaOf(editing, "zwei").getMinY(), lastPointOfLine(editing).getY(), 0.5);
  }

  /** The eaten waypoint is back when the box is moved away from it again. */
  @Test
  void waypointComesBackWhenTheBoxMovesAway() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(WITH_WAYPOINT, host);
    Point2D press = centerOf(editing, "zwei");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);

    editing.dragTo(new Point2D.Double(press.getX() - 280, press.getY()));
    assertEquals(2, editing.scene().relationLines.get(0).points().size());
    editing.dragTo(new Point2D.Double(press.getX() - 100, press.getY()));

    assertEquals(3, editing.scene().relationLines.get(0).points().size());
  }

  /** The waypoint stays eaten after the box is released. */
  @Test
  void eatenWaypointStaysGone() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(WITH_WAYPOINT, host);

    dragZwei(editing, -280, 0);
    editing.endDrag();

    assertTrue(routeOf(editing).waypoints.isEmpty());
    assertEquals(1, host.reportedSources.size());
  }

  /** A waypoint under a box which the line doesn't end at is not eaten. */
  @Test
  void otherBoxesEatNothing() throws ClassDiagramParseException {
    String source = WITH_WAYPOINT.replace("(40, 210)", "(40, 210) (650, 440)")
      .replace("}", "  class drei (33333333) at(640, 430)\n}");
    DiagramEditing editing = new DiagramEditing(source, host);

    dragZwei(editing, 5, 0);

    assertEquals(4, editing.scene().relationLines.get(0).points().size());
  }

  /** The waypoints of anchor lines are eaten like those of relations. */
  @Test
  void anchorLineWaypointIsEaten() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (33333333) at(10, 20) \"eins\"\n  class zwei (22222222) at(300, 200)\n"
      + "  anchor 33333333 -- 22222222 route (40, 210)\n}";
    DiagramEditing editing = new DiagramEditing(source, host);
    assertEquals(3, editing.scene().attachLines.get(0).points().size());

    dragZwei(editing, -280, 0);

    assertEquals(2, editing.scene().attachLines.get(0).points().size());
  }

  private void dragZwei(DiagramEditing editing, double dx, double dy) {
    Point2D press = centerOf(editing, "zwei");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + dx, press.getY() + dy));
  }

  private void dragEins(DiagramEditing editing, double dx, double dy) {
    Point2D press = centerOf(editing, "eins");
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + dx, press.getY() + dy));
  }

  private void dragEinsAndRelease(DiagramEditing editing, double dx, double dy) {
    dragEins(editing, dx, dy);
    editing.endDrag();
  }

  private static classman.model.v001.RouteModel_V001 routeOf(DiagramEditing editing) {
    try {
      return new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).route;
    }
    catch (ClassDiagramParseException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Point2D centerOf(DiagramEditing editing, String className) {
    Rectangle2D area = areaOf(editing, className);
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private static Rectangle2D areaOf(DiagramEditing editing, String className) {
    return editing.scene().elementAreas.stream()
      .filter(candidate -> candidate.element instanceof ClassModel_V001 box && box.name.equals(className)
        || candidate.element instanceof classman.model.v001.NoteModel_V001 note && note.text.equals(className))
      .findFirst().orElseThrow().area;
  }
}
