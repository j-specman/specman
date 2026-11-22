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

class DiagramMovingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String TWO_PLACED = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(400, 20)\n}";
  private static final String TWO_STACKED = "classdiagram {\n  class Book (04de91aa)\n  class Page (11111111)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The box follows the mouse and stays where it was dropped. */
  @Test
  void movedBox() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_PLACED, host);
    select(editing, 15, 25);

    editing.startDrag(new Point2D.Double(15, 25));
    editing.dragTo(new Point2D.Double(115, 75));

    assertEquals(110, editing.selectedArea().getX(), 0.001);
    assertEquals(70, editing.selectedArea().getY(), 0.001);

    editing.endDrag();

    ClassModel_V001 book = classNamed(editing.source(), "Book");
    assertEquals(110, book.position.x);
    assertEquals(70, book.position.y);
    assertEquals(400, classNamed(editing.source(), "Page").position.x);
  }

  /** Nothing is reported while the mouse is dragged, and only one source when it is released. */
  @Test
  void reportedOnRelease() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_PLACED, host);
    select(editing, 15, 25);

    editing.startDrag(new Point2D.Double(15, 25));
    editing.dragTo(new Point2D.Double(50, 25));
    editing.dragTo(new Point2D.Double(90, 25));
    editing.dragTo(new Point2D.Double(115, 25));
    assertTrue(host.reportedSources.isEmpty());

    editing.endDrag();

    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** A press without a drag moves nothing and is no operation. */
  @Test
  void clickIsNoMove() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_PLACED, host);
    select(editing, 15, 25);

    editing.startDrag(new Point2D.Double(15, 25));
    editing.endDrag();

    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** A press on nothing drags nothing. */
  @Test
  void nothingToDrag() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_PLACED, host);
    select(editing, 250, 300);

    editing.startDrag(new Point2D.Double(250, 300));
    editing.dragTo(new Point2D.Double(300, 300));
    editing.endDrag();

    assertTrue(host.reportedSources.isEmpty());
  }

  /** Esc during a drag puts the box back, and the release afterwards reports nothing. */
  @Test
  void cancelledDrag() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_PLACED, host);
    select(editing, 15, 25);
    editing.startDrag(new Point2D.Double(15, 25));
    editing.dragTo(new Point2D.Double(115, 75));

    assertTrue(editing.cancelDrag());

    assertEquals(10, editing.selectedArea().getX(), 0.001);
    assertEquals(20, editing.selectedArea().getY(), 0.001);
    editing.endDrag();
    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.cancelDrag());
  }

  /** Moving one of the stacked boxes leaves the others where they are, as the layout had them. */
  @Test
  void stackedBoxesStay() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(TWO_STACKED, host);
    Rectangle2D pageBefore = areaOfClassAt(editing, 1);
    Rectangle2D bookBefore = areaOfClassAt(editing, 0);
    select(editing, bookBefore.getCenterX(), bookBefore.getCenterY());

    editing.startDrag(new Point2D.Double(bookBefore.getCenterX(), bookBefore.getCenterY()));
    editing.dragTo(new Point2D.Double(bookBefore.getCenterX() + 300, bookBefore.getCenterY()));
    editing.endDrag();

    assertEquals(pageBefore.getX(), areaOfClassAt(editing, 1).getX(), 0.5);
    assertEquals(pageBefore.getY(), areaOfClassAt(editing, 1).getY(), 0.5);
    assertEquals(bookBefore.getX() + 300, areaOfClassAt(editing, 0).getX(), 0.5);
    assertEquals(bookBefore.getY(), areaOfClassAt(editing, 0).getY(), 0.5);
  }

  private static void select(DiagramEditing editing, double x, double y) {
    editing.selectAt(new Point2D.Double(x, y));
  }

  private static Rectangle2D areaOfClassAt(DiagramEditing editing, int index) {
    SceneElementArea area = editing.scene().elementAreas.get(index);
    return area.area;
  }

  private static ClassModel_V001 classNamed(String source, String name) throws ClassDiagramParseException {
    for (ClassModel_V001 box : new ClassDiagramParser_V001().parse(source).classes()) {
      if (name.equals(box.name)) {
        return box;
      }
    }
    throw new AssertionError("no class " + name + " in " + source);
  }
}
