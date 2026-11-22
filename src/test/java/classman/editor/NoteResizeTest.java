package classman.editor;

import classman.model.v001.NoteModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteResizeTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String FIXED = "classdiagram {\n  note (11111111) at(10, 20) width=120 \"alpha beta gamma delta\"\n}";
  private static final String AUTOMATIC = "classdiagram {\n  note (11111111) at(10, 20) \"alpha beta gamma delta\"\n}";
  private static final String STACKED = "classdiagram {\n  class A (22222222)\n  note (11111111) \"alpha beta\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
  }

  private Rectangle2D noteArea() {
    return editing.scene().elementAreas.get(editing.scene().elementAreas.size() - 1).area;
  }

  private Point2D leftEdge() {
    return new Point2D.Double(noteArea().getMinX(), noteArea().getCenterY());
  }

  private Point2D rightEdge() {
    return new Point2D.Double(noteArea().getMaxX(), noteArea().getCenterY());
  }

  private void drag(Point2D from, double toX) {
    editing.selectAt(from, TOLERANCE);
    editing.startDrag(from, TOLERANCE);
    editing.dragTo(new Point2D.Double(toX, from.getY()));
    editing.endDrag();
  }

  private NoteModel_V001 noteOf(int index) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).notes().get(index);
  }

  /** The sides of a note are where its width is changed, and the cursor shows it. */
  @Test
  void sidesOfNote() throws ClassDiagramParseException {
    open(FIXED);
    PointerControl control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);

    ResizeTarget right = editing.resizeTargetAt(rightEdge(), TOLERANCE);
    ResizeTarget left = editing.resizeTargetAt(leftEdge(), TOLERANCE);

    assertEquals(ResizeSide.RIGHT, right.side);
    assertEquals(ResizeSide.LEFT, left.side);
    assertEquals(NoteModel_V001.class, right.box.getClass());
    assertEquals(Cursor.E_RESIZE_CURSOR, control.cursorAt(rightEdge()).getType());
    assertNull(editing.resizeTargetAt(new Point2D.Double(noteArea().getCenterX(), noteArea().getCenterY()), TOLERANCE));
  }

  /** Dragging the right side of a note with automatic width gives it a width of its own, the text wraps at it. */
  @Test
  void dragRightSide() throws ClassDiagramParseException {
    open(AUTOMATIC);
    double heightBefore = noteArea().getHeight();

    drag(rightEdge(), 110);

    assertEquals(100, noteOf(0).width);
    assertEquals(10, noteOf(0).position.x);
    assertEquals(100, noteArea().getWidth(), 1);
    assertTrue(noteArea().getHeight() > heightBefore);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** Dragging the left side keeps the right side where it is. */
  @Test
  void dragLeftSide() throws ClassDiagramParseException {
    open(FIXED);

    drag(leftEdge(), -20);

    assertEquals(150, noteOf(0).width);
    assertEquals(-20, noteOf(0).position.x);
  }

  /** The note doesn't get narrower than its longest word. */
  @Test
  void minimumWidth() throws ClassDiagramParseException {
    open(FIXED);

    drag(rightEdge(), -500);

    assertEquals(noteArea().getWidth(), noteOf(0).width, 1);
    assertTrue(noteOf(0).width > 40, "the longest word still fits: " + noteOf(0).width);
  }

  /** The other boxes stay where they are while a stacked note is resized. */
  @Test
  void stackedBoxesStayPut() throws ClassDiagramParseException {
    open(STACKED);
    Rectangle2D classBefore = editing.scene().elementAreas.get(0).area;

    drag(rightEdge(), noteArea().getMaxX() + 50);

    assertEquals(Math.round(classBefore.getX()), new ClassDiagramParser_V001().parse(editing.source()).classes().get(0).position.x);
    assertTrue(noteOf(0).width > 0);
  }

  /** The automatic width is switched on again with one operation. */
  @Test
  void resetWidth() throws ClassDiagramParseException {
    open(FIXED);
    editing.selectAt(new Point2D.Double(noteArea().getCenterX(), noteArea().getCenterY()));
    assertTrue(editing.hasFixedWidth(editing.selectedNote()));

    editing.resetWidth(editing.selectedNote());

    assertNull(noteOf(0).width);
    assertFalse(editing.hasFixedWidth(editing.selectedNote()));
    assertEquals(1, host.reportedSources.size());
  }
}
