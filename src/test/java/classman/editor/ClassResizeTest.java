package classman.editor;

import classman.model.v001.RelationType_V001;
import classman.model.v001.ClassModel_V001;
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

class ClassResizeTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String FIXED = "classdiagram {\n  class A (11111111) at(10, 20) width=100 { x }\n}";
  private static final String STACKED = "classdiagram {\n  class A (11111111)\n  class B (22222222)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
  }

  private void selectFirstClass() {
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(area.getCenterX(), area.getMaxY() - 2));
  }

  private Point2D leftEdge() {
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    return new Point2D.Double(area.getMinX(), area.getCenterY());
  }

  private Point2D rightEdge() {
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    return new Point2D.Double(area.getMaxX(), area.getCenterY());
  }

  private Cursor cursorAt(Point2D point) {
    return new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels).cursorAt(point);
  }

  private void drag(Point2D from, double toX) {
    editing.startDrag(from, TOLERANCE);
    editing.dragTo(new Point2D.Double(toX, from.getY()));
    editing.endDrag();
  }

  private ClassModel_V001 classOf(int index) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(index);
  }

  /** The left and the right side of a class are where its width is changed, along its whole height and a little
   * beyond the border to both sides. Nothing else is. */
  @Test
  void sidesOfClass() throws ClassDiagramParseException {
    open(FIXED);
    Rectangle2D area = editing.scene().elementAreas.get(0).area;

    assertEquals(ResizeSide.RIGHT, editing.resizeTargetAt(rightEdge(), TOLERANCE).side);
    assertEquals(ResizeSide.RIGHT, editing.resizeTargetAt(new Point2D.Double(area.getMaxX() + 3, area.getMinY()),
      TOLERANCE).side);
    assertEquals(ResizeSide.LEFT, editing.resizeTargetAt(new Point2D.Double(area.getMinX() - 3, area.getMaxY()),
      TOLERANCE).side);
    assertNull(editing.resizeTargetAt(new Point2D.Double(area.getCenterX(), area.getCenterY()), TOLERANCE));
    assertNull(editing.resizeTargetAt(new Point2D.Double(area.getMaxX() + 6, area.getCenterY()), TOLERANCE));
    assertNull(editing.resizeTargetAt(new Point2D.Double(area.getMaxX(), area.getMinY() - 6), TOLERANCE));
  }

  /** The cursor shows the sides, and nothing else. No selection is needed. */
  @Test
  void cursorAtSides() throws ClassDiagramParseException {
    open(FIXED);
    Rectangle2D area = editing.scene().elementAreas.get(0).area;

    assertEquals(Cursor.E_RESIZE_CURSOR, cursorAt(rightEdge()).getType());
    assertEquals(Cursor.W_RESIZE_CURSOR, cursorAt(leftEdge()).getType());
    assertEquals(Cursor.DEFAULT_CURSOR, cursorAt(new Point2D.Double(area.getCenterX(), area.getCenterY())).getType());

    editing.toggleRelationTool(RelationType_V001.ASSOCIATION);

    assertEquals(Cursor.CROSSHAIR_CURSOR, cursorAt(rightEdge()).getType());
  }

  /** A press at the side of a class, also a little outside of it, selects the class. */
  @Test
  void pressAtSideSelectsClass() throws ClassDiagramParseException {
    open(FIXED);
    Point2D outside = new Point2D.Double(rightEdge().getX() + 3, rightEdge().getY());

    editing.selectAt(outside, TOLERANCE);

    assertEquals("A", editing.selectedClass().name);
  }

  /** Dragging the right side gives the class a width of its own, the left side stays, one undo step. */
  @Test
  void dragRightSide() throws ClassDiagramParseException {
    open(FIXED);
    selectFirstClass();

    drag(rightEdge(), 170);

    assertEquals(160, classOf(0).width);
    assertEquals(10, classOf(0).position.x);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** Dragging the left side keeps the right side where it is. */
  @Test
  void dragLeftSide() throws ClassDiagramParseException {
    open(FIXED);
    selectFirstClass();

    drag(leftEdge(), -20);

    assertEquals(130, classOf(0).width);
    assertEquals(-20, classOf(0).position.x);
  }

  /** The class doesn't get narrower than its layout allows, and what is stored is what is shown. */
  @Test
  void minimumWidth() throws ClassDiagramParseException {
    open("classdiagram {\n  class Abcdefghij (11111111) at(10, 20) width=200\n}");
    selectFirstClass();

    drag(rightEdge(), -500);

    assertEquals(editing.selectedArea().getWidth(), classOf(0).width, 1);
    assertTrue(classOf(0).width > 60, "the name still fits: " + classOf(0).width);
    assertEquals(10, classOf(0).position.x);
  }

  /** A class which is stacked stays where it is when it is resized, and so does the other one. */
  @Test
  void stackedClassesStayPut() throws ClassDiagramParseException {
    open(STACKED);
    Rectangle2D firstBefore = editing.scene().elementAreas.get(0).area;
    Rectangle2D secondBefore = editing.scene().elementAreas.get(1).area;
    selectFirstClass();

    drag(rightEdge(), firstBefore.getMaxX() + 50);

    assertEquals(Math.round(firstBefore.getX()), classOf(0).position.x);
    assertEquals(Math.round(secondBefore.getX()), classOf(1).position.x);
    assertEquals(Math.round(secondBefore.getY()), classOf(1).position.y);
    assertNull(classOf(1).width);
  }

  /** Esc during the drag puts the class back as it was. */
  @Test
  void cancelPutsClassBack() throws ClassDiagramParseException {
    open(STACKED);
    selectFirstClass();
    editing.startDrag(rightEdge(), TOLERANCE);
    editing.dragTo(new Point2D.Double(rightEdge().getX() + 80, rightEdge().getY()));

    assertTrue(editing.cancelDrag());

    assertEquals(STACKED, editing.source());
    assertNull(editing.selectedClass().width);
    assertNull(editing.selectedClass().position);
    assertFalse(editing.canUndo());
  }

  /** The width is no change which is marked, with or without change tracking. */
  @Test
  void widthIsNeverMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(FIXED);
    selectFirstClass();

    drag(rightEdge(), 170);

    assertFalse(editing.source().contains("change("), editing.source());
    assertFalse(editing.source().contains("was("), editing.source());
  }

  /** The automatic width is switched on again with one operation, and there is nothing to do if it is on. */
  @Test
  void resetWidth() throws ClassDiagramParseException {
    open(FIXED);
    selectFirstClass();
    assertTrue(editing.hasFixedWidth(editing.selectedClass()));

    editing.resetWidth(editing.selectedClass());

    assertNull(classOf(0).width);
    assertFalse(editing.hasFixedWidth(editing.selectedClass()));
    assertEquals(1, host.reportedSources.size());

    editing.resetWidth(editing.selectedClass());

    assertEquals(1, host.reportedSources.size());
  }
}
