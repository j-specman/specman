package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneElementArea;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupMovingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String THREE = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(300, 20)\n  class Cover (22222222) at(10, 400)\n}";
  private static final String THREE_STACKED = "classdiagram {\n  class Book (04de91aa)\n  class Page (11111111)\n"
    + "  class Cover (22222222)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** All boxes of the group move by the distance of the mouse, the others stay. It is one operation. */
  @Test
  void groupMovesTogether() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    selectBookAndPage(editing);

    drag(editing, "Book", 50, 30);

    assertEquals(60, positionOf(editing, "Book").x);
    assertEquals(50, positionOf(editing, "Book").y);
    assertEquals(350, positionOf(editing, "Page").x);
    assertEquals(50, positionOf(editing, "Page").y);
    assertEquals(10, positionOf(editing, "Cover").x);
    assertEquals(400, positionOf(editing, "Cover").y);
    assertEquals(1, host.reportedSources.size());
    assertEquals(2, editing.selectedBoxes().size());
  }

  /** The group stops as a whole at the left and upper edge, so the boxes keep their distances. */
  @Test
  void groupStopsAtTopLeftCorner() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    selectBookAndPage(editing);

    drag(editing, "Book", -100, -100);

    assertEquals(0, positionOf(editing, "Book").x);
    assertEquals(0, positionOf(editing, "Book").y);
    assertEquals(290, positionOf(editing, "Page").x);
    assertEquals(0, positionOf(editing, "Page").y);
  }

  /** A click on a member without a drag makes it the only selected box and changes nothing. */
  @Test
  void clickReducesTheSelectionToOneBox() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    selectBookAndPage(editing);
    Point2D onPage = centerOf(editing, "Page");

    editing.selectAt(onPage, TOLERANCE);
    editing.startDrag(onPage, TOLERANCE);
    editing.endDrag();

    assertEquals("Page", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** Esc during the drag puts the whole group back. */
  @Test
  void escapeBringsTheGroupBack() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    selectBookAndPage(editing);
    Point2D onBook = centerOf(editing, "Book");

    editing.selectAt(onBook, TOLERANCE);
    editing.startDrag(onBook, TOLERANCE);
    editing.dragTo(new Point2D.Double(onBook.getX() + 80, onBook.getY() + 80));
    assertTrue(editing.cancelDrag());
    editing.endDrag();

    assertEquals(10, positionOf(editing, "Book").x);
    assertEquals(300, positionOf(editing, "Page").x);
    assertEquals(2, editing.selectedBoxes().size());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** A press on a box outside of the group selects that box alone. */
  @Test
  void pressOutsideTheGroupSelectsThatBox() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    selectBookAndPage(editing);

    editing.selectAt(centerOf(editing, "Cover"), TOLERANCE);

    assertEquals("Cover", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());
  }

  /** Boxes without a position, which the layout stacks, keep their places (within the rounding to whole units) when a group of them is moved. */
  @Test
  void stackedBoxesKeepTheirPlaces() throws Exception {
    DiagramEditing editing = new DiagramEditing(THREE_STACKED, host);
    Rectangle2D coverBefore = areaOf(editing, "Cover");
    Rectangle2D book = areaOf(editing, "Book");
    Rectangle2D page = areaOf(editing, "Page");
    pullBand(editing, book.getX() - 5, book.getY() - 5, page.getMaxX() + 5, page.getMaxY() + 5);
    assertEquals(2, editing.selectedBoxes().size());

    drag(editing, "Book", 40, 0);

    assertEquals(coverBefore.getX(), areaOf(editing, "Cover").getX(), 1);
    assertEquals(coverBefore.getY(), areaOf(editing, "Cover").getY(), 1);
    assertFalse(areaOf(editing, "Book").intersects(areaOf(editing, "Page")));
  }

  private void selectBookAndPage(DiagramEditing editing) {
    pullBand(editing, 0, 0, 700, 250);
    assertEquals(2, editing.selectedBoxes().size());
  }

  private static void pullBand(DiagramEditing editing, double x1, double y1, double x2, double y2) {
    Point2D press = new Point2D.Double(x1, y1);
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.startRubberBand(press);
    editing.dragTo(new Point2D.Double(x2, y2));
    editing.endDrag();
  }

  private static void drag(DiagramEditing editing, String className, double dx, double dy) {
    Point2D press = centerOf(editing, className);
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.dragTo(new Point2D.Double(press.getX() + dx, press.getY() + dy));
    editing.endDrag();
  }

  private static Point2D centerOf(DiagramEditing editing, String className) {
    Rectangle2D area = areaOf(editing, className);
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private static Rectangle2D areaOf(DiagramEditing editing, String className) {
    return editing.scene().elementAreas.stream()
      .filter((SceneElementArea area) -> area.element instanceof ClassModel_V001 box && box.name.equals(className))
      .findFirst().orElseThrow().area;
  }

  private static classman.model.v001.PositionModel_V001 positionOf(DiagramEditing editing, String className)
    throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().stream()
      .filter(box -> box.name.equals(className)).findFirst().orElseThrow().position;
  }
}
