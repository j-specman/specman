package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The canvas shows the diagram from a fixed point, so that the places of the elements on the screen only change when
 * the user moves them. */
class DiagramCanvasOriginTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String PLACED = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(400, 300)\n}";
  private static final String BEYOND_ZERO = "classdiagram {\n  class Book (04de91aa) at(-50, -40)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The top left corner of the canvas is the corner of the diagram, (0, 0), with a margin. */
  @Test
  void originIsTheCornerOfTheDiagram() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(PLACED, host);

    DiagramCanvas canvas = new DiagramCanvas(editing.scene());

    assertEquals(new Point2D.Double(-10, -10), canvas.toDiagram(new Point(0, 0)));
  }

  /** The top left box is moved away from the corner: the canvas doesn't follow it, nothing moves on the screen except
   * the box. */
  @Test
  void originStaysWhenTheTopLeftBoxMoves() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(PLACED, host);
    DiagramCanvas canvas = new DiagramCanvas(editing.scene());
    Point2D bookOnScreen = new Point2D.Double(15, 25);
    editing.selectAt(bookOnScreen);

    editing.startDrag(bookOnScreen);
    editing.dragTo(new Point2D.Double(515, 425));
    editing.endDrag();
    canvas.setScene(editing.scene());

    assertEquals(new Point2D.Double(-10, -10), canvas.toDiagram(new Point(0, 0)));
    assertEquals(new Point2D.Double(510, 420),
      new Point2D.Double(editing.selectedArea().getX(), editing.selectedArea().getY()));
  }

  /** A diagram which reaches into negative coordinates, made before there was a limit, is shown completely. */
  @Test
  void oldDiagramBeyondZeroIsShown() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(BEYOND_ZERO, host);

    DiagramCanvas canvas = new DiagramCanvas(editing.scene());

    Point2D origin = canvas.toDiagram(new Point(0, 0));
    assertEquals(-60, origin.getX(), 0.001);
    assertEquals(-50, origin.getY(), 0.001);
  }

  /** Undo and redo don't move the canvas either. */
  @Test
  void originStaysAfterANewScene() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(PLACED, host);
    DiagramCanvas canvas = new DiagramCanvas(editing.scene());
    DiagramEditing other = new DiagramEditing(BEYOND_ZERO, host);

    canvas.setScene(other.scene());

    assertEquals(new Point2D.Double(-10, -10), canvas.toDiagram(new Point(0, 0)));
  }
}
