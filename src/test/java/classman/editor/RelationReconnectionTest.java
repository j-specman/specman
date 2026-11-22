package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationReconnectionTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  class C (33333333) at(300, 250)\n";
  private static final String ROUTED = CLASSES + "  association (44444444) 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}";
  private static final String STRAIGHT = CLASSES + "  association (44444444) 11111111 -- 22222222\n}";
  private static final double TOLERANCE = 4;
  private static final double CAPTURE = 6;

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** The end hangs on the class which the mouse is on, at the closest point of its border. */
  @Test
  void endHungOntoOtherClass() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getX() + 20, classC.getCenterY()), CAPTURE);
    editing.endDrag();

    assertEquals("33333333", relationOf(editing.source()).toId);
    assertEquals("33333333", relationOf(editing.source()).route.end.classId);
    assertEquals("11111111", relationOf(editing.source()).fromId);
    assertEquals(1, host.reportedSources.size());
  }

  /** The start can be hung onto another class as well. */
  @Test
  void startHungOntoOtherClass() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(editing.selectedHandles().get(0), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    editing.endDrag();

    assertEquals("33333333", relationOf(editing.source()).fromId);
    assertEquals("22222222", relationOf(editing.source()).toId);
  }

  /** Close to a class, though not on it, is enough. */
  @Test
  void closeToClassIsEnough() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getMinY() - 5), CAPTURE);
    editing.endDrag();

    assertEquals("33333333", relationOf(editing.source()).toId);
  }

  /** The class which the end will hang on is framed while the mouse is on it. */
  @Test
  void targetFramedWhileHovering() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    assertNull(editing.connectionTargetArea());
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);

    assertEquals(classC, editing.connectionTargetArea());

    editing.endDrag();
    assertNull(editing.connectionTargetArea());
  }

  /** In the open the end keeps the class it caught last, so the line doesn't jump back and forth between classes. */
  @Test
  void endKeepsLastCaughtClass() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    editing.dragTo(new Point2D.Double(600, 140), CAPTURE);

    assertEquals(classC, editing.connectionTargetArea());
    editing.endDrag();
    assertEquals("33333333", relationOf(editing.source()).toId);
  }

  /** Catching the own class again takes the end back to it. */
  @Test
  void backToOwnClass() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classB = areaOfClassAt(editing, 305, 25);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    editing.dragTo(new Point2D.Double(classB.getCenterX(), classB.getCenterY()), CAPTURE);
    editing.endDrag();

    assertEquals("22222222", relationOf(editing.source()).toId);
    assertNull(editing.connectionTargetArea());
  }

  /** In the open without having caught a class, the end stays on its own. */
  @Test
  void openSpaceChangesNothing() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(600, 140), CAPTURE);
    editing.endDrag();

    assertEquals("22222222", relationOf(editing.source()).toId);
    assertNull(editing.connectionTargetArea());
  }

  /** The own class is no target either while the mouse is on it. */
  @Test
  void ownClassIsNoTarget() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classB = areaOfClassAt(editing, 305, 25);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classB.getCenterX(), classB.getCenterY()), CAPTURE);

    assertNull(editing.connectionTargetArea());
    assertEquals("22222222", relationOf(editing.source()).toId);
  }

  /** Esc puts the class back, nothing is reported. */
  @Test
  void cancelPutsClassBack() throws ClassDiagramParseException {
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);
    Point2D endBefore = endOf(editing);

    editing.startDrag(endBefore, TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    boolean cancelled = editing.cancelDrag();

    assertTrue(cancelled);
    assertEquals(endBefore, endOf(editing));
    assertTrue(host.reportedSources.isEmpty());
    assertNull(editing.connectionTargetArea());
  }

  /** While changes are tracked, an end stays on its class, but can still slide along its border. */
  @Test
  void notWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = selectedAt(ROUTED);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(endOf(editing), TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    assertNull(editing.connectionTargetArea());
    editing.endDrag();

    assertEquals("22222222", relationOf(editing.source()).toId);
    assertEquals("22222222", relationOf(editing.source()).route.end.classId);
    assertNull(relationOf(editing.source()).change);
  }

  /** A relation without a route gets one, and the cancelled drag takes it away again. */
  @Test
  void relationWithoutRoute() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(STRAIGHT, host);
    Point2D start = editing.scene().relationLines.get(0).points().get(0);
    Point2D end = editing.scene().relationLines.get(0).points().get(1);
    editing.selectAt(new Point2D.Double((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2), TOLERANCE);
    Rectangle2D classC = areaOfClassAt(editing, 305, 255);

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    editing.cancelDrag();
    assertFalse(editing.canUndo());
    assertEquals(end, endOf(editing));

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(new Point2D.Double(classC.getCenterX(), classC.getCenterY()), CAPTURE);
    editing.endDrag();

    assertEquals("33333333", relationOf(editing.source()).toId);
    assertEquals(0, relationOf(editing.source()).route.waypoints.size());
  }

  private DiagramEditing selectedAt(String source) throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(source, host);
    editing.selectAt(new Point2D.Double(100, 150), TOLERANCE);
    return editing;
  }

  private static Point2D endOf(DiagramEditing editing) {
    return editing.selectedHandles().get(editing.selectedHandles().size() - 1);
  }

  private static Rectangle2D areaOfClassAt(DiagramEditing editing, double x, double y) {
    return editing.scene().elementAreaAt(new Point2D.Double(x, y)).area;
  }

  private static AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).relations().get(0);
  }
}
