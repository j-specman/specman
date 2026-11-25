package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubberBandSelectionTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String THREE = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(300, 20)\n  class Cover (22222222) at(10, 400)\n"
    + "  association (33333333) 04de91aa 1 -> * 11111111\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The boxes which lie completely in the rectangle are selected together, each with a frame. */
  @Test
  void boxesInsideAreSelectedTogether() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);

    pullBand(editing, 0, 0, 700, 250);

    assertEquals(2, editing.selectedBoxes().size());
    assertEquals(2, editing.selectionMarks().frames.size());
    assertNull(editing.selectedClass());
    assertTrue(editing.hasSelection());
  }

  /** While the rectangle is pulled open, the boxes in it already show their frames, though nothing is selected yet. */
  @Test
  void framesFollowTheBandWhileDragging() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    startBand(editing, 0, 0);

    editing.dragTo(new Point2D.Double(700, 250));

    assertEquals(2, editing.selectionMarks().frames.size());
    assertFalse(editing.hasSelection());

    editing.dragTo(new Point2D.Double(areaOf(editing, "Page").getCenterX(), 250));

    assertEquals(1, editing.selectionMarks().frames.size());
  }

  /** A box which the rectangle only touches is not selected. */
  @Test
  void touchedBoxIsNotSelected() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    Rectangle2D page = areaOf(editing, "Page");

    pullBand(editing, 0, 0, page.getCenterX(), 250);

    assertEquals("Book", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());
  }

  /** A rectangle around one box selects it like a click does. */
  @Test
  void oneBoxIsASingleSelection() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);

    pullBand(editing, 0, 380, 400, 700);

    assertEquals("Cover", editing.selectedClass().name);
    assertTrue(editing.selectedBoxes().isEmpty());
  }

  /** A rectangle with no box in it selects nothing. */
  @Test
  void emptyBandSelectsNothing() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);

    pullBand(editing, 150, 300, 250, 350);

    assertFalse(editing.hasSelection());
  }

  /** A press on empty space ends the selection of several boxes. */
  @Test
  void pressOnEmptySpaceDeselectsTheGroup() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    pullBand(editing, 0, 0, 700, 250);

    editing.selectAt(new Point2D.Double(500, 500), TOLERANCE);

    assertFalse(editing.hasSelection());
    assertTrue(editing.selectedBoxes().isEmpty());
  }

  /** Esc while the rectangle is pulled open takes it away and selects nothing. */
  @Test
  void escapeCancelsTheBand() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    startBand(editing, 0, 0);
    editing.dragTo(new Point2D.Double(700, 250));
    assertNotNull(editing.selectionMarks().rubberBand);

    assertTrue(editing.cancelDrag());
    editing.endDrag();

    assertNull(editing.selectionMarks().rubberBand);
    assertFalse(editing.hasSelection());
  }

  /** Selecting boxes is no change of the diagram: nothing is reported to the host. */
  @Test
  void selectingReportsNothing() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);

    pullBand(editing, 0, 0, 700, 250);

    assertTrue(host.reportedSources.isEmpty());
  }

  /** A press on a relation selects it and starts no rectangle. */
  @Test
  void pressOnRelationStartsNoBand() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(THREE, host);
    Rectangle2D book = areaOf(editing, "Book");
    Rectangle2D page = areaOf(editing, "Page");
    double lineX = (book.getMaxX() + page.getMinX()) / 2;
    double lineY = (book.getCenterY() + page.getCenterY()) / 2;

    startBand(editing, lineX, lineY);

    assertNotNull(editing.selectedRelation());
    assertNull(editing.selectionMarks().rubberBand);
  }

  private static void startBand(DiagramEditing editing, double x, double y) {
    Point2D press = new Point2D.Double(x, y);
    editing.selectAt(press, TOLERANCE);
    editing.startDrag(press, TOLERANCE);
    editing.startRubberBand(press);
  }

  private static void pullBand(DiagramEditing editing, double x1, double y1, double x2, double y2) {
    startBand(editing, x1, y1);
    editing.dragTo(new Point2D.Double(x2, y2));
    editing.endDrag();
  }

  private static Rectangle2D areaOf(DiagramEditing editing, String className) {
    return editing.scene().elementAreas.stream()
      .filter(area -> area.element instanceof ClassModel_V001 box && box.name.equals(className))
      .findFirst().orElseThrow().area;
  }
}
