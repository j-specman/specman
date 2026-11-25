package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Moving the label and the multiplicities of a relation with the mouse. */
class TextMoveTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String SOURCE = "classdiagram {\n  class A (11111111) at(10, 120)\n"
    + "  class B (22222222) at(400, 120)\n"
    + "  association (33333333) 11111111 1 -> * 22222222 \"owns\"\n}";
  private static final double TOLERANCE = 4;
  private static final double SNAP = 6;

  private final RecordingEditorHost host = new RecordingEditorHost();

  private static Point2D middleOf(Rectangle2D area) {
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private static Rectangle2D areaOf(DiagramEditing editing, TextKind kind) {
    return editing.scene().textAreas.stream().filter(text -> text.kind == kind).findFirst().get().area;
  }

  private static AssociationRelationModel_V001 relationOf(DiagramEditing editing) throws ClassDiagramParseException {
    return (AssociationRelationModel_V001) new ClassDiagramParser_V001().parse(editing.source()).relations().get(0);
  }

  private static void drag(DiagramEditing editing, TextKind kind, double dx, double dy, double snap) {
    Point2D start = middleOf(areaOf(editing, kind));
    editing.selectAt(start, TOLERANCE);
    editing.startDrag(start, TOLERANCE);
    editing.dragTo(new Point2D.Double(start.getX() + dx, start.getY() + dy), snap);
    editing.endDrag();
  }

  /** The label follows the mouse, and the place is kept in the source. */
  @Test
  void labelFollowsMouse() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D before = middleOf(areaOf(editing, TextKind.RELATION_LABEL));

    drag(editing, TextKind.RELATION_LABEL, 0, -30, 0);

    Point2D after = middleOf(areaOf(editing, TextKind.RELATION_LABEL));
    assertEquals(before.getX(), after.getX(), 1.5);
    assertEquals(before.getY() - 30, after.getY(), 1.5);
    assertNotNull(relationOf(editing).labelPlace);
    assertTrue(editing.source().contains("\"owns\" at("), editing.source());
  }

  /** A label which comes close to the middle of the line snaps to it. */
  @Test
  void labelSnapsToMiddle() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);

    drag(editing, TextKind.RELATION_LABEL, 3, -40, SNAP);

    assertEquals(50, relationOf(editing).labelPlace.along);
  }

  /** A text can't be dragged left of or above the top left corner of the diagram, which is at (0, 0). */
  @Test
  void textStopsAtTopLeftCorner() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);

    drag(editing, TextKind.RELATION_LABEL, -2000, -2000, 0);

    Rectangle2D area = areaOf(editing, TextKind.RELATION_LABEL);
    assertTrue(area.getMinX() >= -1, "left edge " + area.getMinX());
    assertTrue(area.getMinY() >= -1, "top edge " + area.getMinY());
  }

  /** A multiplicity moves along the line from its end, and the other one stays where it is. */
  @Test
  void multiplicityMoves() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D toBefore = middleOf(areaOf(editing, TextKind.TO_MULTIPLICITY));
    Point2D fromBefore = middleOf(areaOf(editing, TextKind.FROM_MULTIPLICITY));

    drag(editing, TextKind.FROM_MULTIPLICITY, 40, -25, 0);

    Point2D fromAfter = middleOf(areaOf(editing, TextKind.FROM_MULTIPLICITY));
    assertEquals(fromBefore.getX() + 40, fromAfter.getX(), 1.5);
    assertEquals(fromBefore.getY() - 25, fromAfter.getY(), 1.5);
    assertEquals(toBefore, middleOf(areaOf(editing, TextKind.TO_MULTIPLICITY)));
    assertNotNull(relationOf(editing).fromMultiplicityPlace);
    assertNull(relationOf(editing).toMultiplicityPlace);
  }

  /** The drag is one operation. */
  @Test
  void oneOperation() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D start = middleOf(areaOf(editing, TextKind.RELATION_LABEL));
    editing.selectAt(start, TOLERANCE);

    editing.startDrag(start, TOLERANCE);
    editing.dragTo(new Point2D.Double(start.getX(), start.getY() - 10));
    editing.dragTo(new Point2D.Double(start.getX(), start.getY() - 20));
    assertTrue(host.reportedSources.isEmpty());
    editing.endDrag();

    assertEquals(1, host.reportedSources.size());
  }

  /** Esc puts the text back where the layout had it, and nothing is reported. */
  @Test
  void cancel() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D before = middleOf(areaOf(editing, TextKind.RELATION_LABEL));
    editing.selectAt(before, TOLERANCE);

    editing.startDrag(before, TOLERANCE);
    editing.dragTo(new Point2D.Double(before.getX() + 30, before.getY() - 30));
    assertTrue(editing.cancelDrag());

    assertEquals(before, middleOf(areaOf(editing, TextKind.RELATION_LABEL)));
    assertTrue(host.reportedSources.isEmpty());
  }

  /** A press on a text of a relation selects the relation, so a drag can start at once. */
  @Test
  void pressOnTextSelectsRelation() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);

    editing.selectAt(middleOf(areaOf(editing, TextKind.RELATION_LABEL)), TOLERANCE);

    assertTrue(editing.isRelationSelected());
  }

  /** A click on a text without a move changes nothing. */
  @Test
  void clickOnTextChangesNothing() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D start = middleOf(areaOf(editing, TextKind.RELATION_LABEL));
    editing.selectAt(start, TOLERANCE);

    editing.startDrag(start, TOLERANCE);
    editing.endDrag();

    assertFalse(editing.canUndo());
  }

  /** The text goes back to the layout with the reset, in one operation of its own. */
  @Test
  void resetPlace() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    Point2D before = middleOf(areaOf(editing, TextKind.RELATION_LABEL));
    drag(editing, TextKind.RELATION_LABEL, 0, -30, 0);
    SceneTextArea moved = editing.movableTextAt(middleOf(areaOf(editing, TextKind.RELATION_LABEL)));
    assertTrue(editing.hasFixedPlace(moved));

    editing.resetPlace(moved);

    assertNull(relationOf(editing).labelPlace);
    assertEquals(before, middleOf(areaOf(editing, TextKind.RELATION_LABEL)));
    assertEquals(2, host.reportedSources.size());
  }

  /** Moving a text is no change which a change set tracks. */
  @Test
  void notMarkedWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(SOURCE, host);

    drag(editing, TextKind.RELATION_LABEL, 0, -30, 0);

    AbstractRelationModel_V001 relation = relationOf(editing);
    assertNull(relation.change);
    assertNull(relation.labelChange);
  }

  /** A text which stands far from its line has a leader line while its relation is selected, and none otherwise. */
  @Test
  void leaderOnlyWhileSelected() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(SOURCE, host);
    assertTrue(editing.selectionMarks().leaders.isEmpty());

    drag(editing, TextKind.RELATION_LABEL, 0, -80, 0);

    assertEquals(1, editing.selectionMarks().leaders.size());
    editing.deselect();
    assertTrue(editing.selectionMarks().leaders.isEmpty());
  }
}
