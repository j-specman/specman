package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CtrlSelectionTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String DIAGRAM = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(300, 20)\n  class Cover (22222222) at(10, 400)\n"
    + "  association 04de91aa -> 11111111\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** Ctrl+click adds boxes one after the other, the first one is a single selection. */
  @Test
  void ctrlClickAddsBoxes() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);

    editing.pressWithCtrl(centerOf(editing, "Book"), TOLERANCE);
    assertEquals("Book", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());

    editing.pressWithCtrl(centerOf(editing, "Page"), TOLERANCE);
    assertEquals(List.of("Book", "Page"), names(editing.selectedBoxes()));
    assertEquals(2, editing.selectionMarks().frames.size());

    editing.pressWithCtrl(centerOf(editing, "Cover"), TOLERANCE);
    assertEquals(List.of("Book", "Page", "Cover"), names(editing.selectedBoxes()));
  }

  /** Ctrl+click on a selected box takes it out; two boxes become one box, one box becomes nothing. */
  @Test
  void ctrlClickTakesBoxesOut() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAllBoxes();

    editing.pressWithCtrl(centerOf(editing, "Page"), TOLERANCE);
    assertEquals(List.of("Book", "Cover"), names(editing.selectedBoxes()));

    editing.pressWithCtrl(centerOf(editing, "Book"), TOLERANCE);
    assertEquals("Cover", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());

    editing.pressWithCtrl(centerOf(editing, "Cover"), TOLERANCE);
    assertFalse(editing.hasSelection());
  }

  /** A relation can't be selected together with boxes: Ctrl+click on it changes nothing, and Ctrl+click on a box
   * while a relation is selected selects the box alone. */
  @Test
  void relationsStayOutOfTheGroup() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.pressWithCtrl(centerOf(editing, "Book"), TOLERANCE);
    Rectangle2D book = areaOf(editing, "Book");
    Rectangle2D page = areaOf(editing, "Page");
    Point2D onLine = new Point2D.Double((book.getMaxX() + page.getMinX()) / 2, (book.getCenterY() + page.getCenterY()) / 2);

    editing.pressWithCtrl(onLine, TOLERANCE);

    assertEquals("Book", editing.selectedClass().name);

    editing.selectAt(onLine, TOLERANCE);
    assertNotNull(editing.selectedRelation());
    editing.pressWithCtrl(centerOf(editing, "Cover"), TOLERANCE);

    assertEquals("Cover", editing.selectedClass().name);
  }

  /** A rectangle with Ctrl adds the boxes in it to the selected one; without being pulled open it changes nothing. */
  @Test
  void ctrlBandAddsToTheSelection() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAt(centerOf(editing, "Cover"), TOLERANCE);

    editing.pressWithCtrl(new Point2D.Double(500, 500), TOLERANCE);
    editing.endDrag();
    assertEquals("Cover", editing.selectedClass().name);

    editing.pressWithCtrl(new Point2D.Double(0, 0), TOLERANCE);
    editing.dragTo(new Point2D.Double(700, 250));
    assertEquals(3, editing.selectionMarks().frames.size());
    editing.endDrag();

    assertEquals(List.of("Book", "Page", "Cover"), names(editing.selectedBoxes()));
  }

  /** A rectangle with Ctrl doesn't take away a box which is selected already, even if it lies in the rectangle. */
  @Test
  void ctrlBandNeverTakesBoxesOut() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.pressWithCtrl(centerOf(editing, "Book"), TOLERANCE);

    editing.pressWithCtrl(new Point2D.Double(0, 0), TOLERANCE);
    editing.dragTo(new Point2D.Double(700, 250));
    editing.endDrag();

    assertEquals(List.of("Book", "Page"), names(editing.selectedBoxes()));
  }

  /** Esc during a rectangle with Ctrl leaves the selection as it was. */
  @Test
  void escapeKeepsTheSelection() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAt(centerOf(editing, "Cover"), TOLERANCE);
    editing.pressWithCtrl(new Point2D.Double(0, 0), TOLERANCE);
    editing.dragTo(new Point2D.Double(700, 250));

    assertTrue(editing.cancelDrag());
    editing.endDrag();

    assertEquals("Cover", editing.selectedClass().name);
  }

  /** Select all takes the boxes and no relation. */
  @Test
  void selectAllBoxes() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);

    editing.selectAllBoxes();

    assertEquals(List.of("Book", "Page", "Cover"), names(editing.selectedBoxes()));
    assertEquals(3, editing.selectionMarks().frames.size());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** In a diagram with one box, select all is that box's single selection, in an empty diagram nothing is selected. */
  @Test
  void selectAllWithFewBoxes() throws ClassDiagramParseException {
    DiagramEditing one = new DiagramEditing("classdiagram {\n  class Book (04de91aa) at(10, 20)\n}", host);
    one.selectAllBoxes();
    assertEquals("Book", one.selectedClass().name);

    DiagramEditing none = new DiagramEditing("classdiagram {\n}", host);
    none.selectAllBoxes();
    assertFalse(none.hasSelection());
  }

  private static List<String> names(List<? extends classman.model.v001.AbstractBoxModel_V001> boxes) {
    return boxes.stream().map(box -> ((ClassModel_V001) box).name).toList();
  }

  private static Point2D centerOf(DiagramEditing editing, String className) {
    Rectangle2D area = areaOf(editing, className);
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private static Rectangle2D areaOf(DiagramEditing editing, String className) {
    return editing.scene().elementAreas.stream()
      .filter(candidate -> candidate.element instanceof ClassModel_V001 box && box.name.equals(className))
      .findFirst().orElseThrow().area;
  }
}
