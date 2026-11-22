package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationCreationTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String TWO_CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host, new EditorTexts(host.locale()));
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
  }

  private Point2D inClass(int index) {
    Rectangle2D area = editing.scene().elementAreas.get(index).area;
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private void draw(Point2D from, Point2D to) {
    control.pressed(from);
    control.dragged(to);
    control.released(to);
  }

  private AbstractRelationModel_V001 relationOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).relations().get(0);
  }

  /** A press on a class and a drag to another one creates a directed association, and the tool is left. */
  @Test
  void associationDrawn() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();

    draw(inClass(0), inClass(1));

    AssociationRelationModel_V001 relation = (AssociationRelationModel_V001) relationOf(editing.source());
    assertEquals(RelationType_V001.ASSOCIATION, relation.type);
    assertEquals("11111111", relation.fromId);
    assertEquals("22222222", relation.toId);
    assertTrue(relation.directed);
    assertNotNull(relation.id);
    assertNull(relation.change);
    assertFalse(editing.isRelationToolArmed());
    assertEquals(relation.id, editing.scene().relationLines.get(0).relation.id);
    assertEquals(2, editing.selectedHandles().size());
    assertTrue(editing.canUndo());
  }

  /** While the mouse is on a class, it is framed and the line ends at its center. */
  @Test
  void targetFramedWhileDrawing() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();
    control.pressed(inClass(0));

    control.dragged(new Point2D.Double(200, 200));
    assertNull(editing.connectionTargetArea());
    assertEquals(200, editing.selectionMarks().connectionLine.getX2());

    control.dragged(inClass(1));
    assertEquals(editing.scene().elementAreas.get(1).area, editing.connectionTargetArea());
    assertEquals(inClass(1).getX(), editing.selectionMarks().connectionLine.getX2());
  }

  /** Released on no class, nothing is created and the tool stays armed; Esc takes back a drawing which is going on. */
  @Test
  void cancelledDrawing() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();

    draw(inClass(0), new Point2D.Double(200, 200));
    assertTrue(editing.isRelationToolArmed());
    assertFalse(editing.canUndo());

    control.pressed(inClass(0));
    control.dragged(inClass(1));
    assertTrue(editing.cancelDrag());
    control.released(inClass(1));

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).relations().isEmpty());
    assertFalse(editing.canUndo());
  }

  /** A press beside the classes starts nothing, and a click on a class without a drag creates nothing. */
  @Test
  void pressOutsideOrWithoutDrag() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();

    draw(new Point2D.Double(200, 200), inClass(1));
    control.pressed(inClass(0));
    control.released(inClass(0));

    assertFalse(editing.canUndo());
    assertTrue(editing.isRelationToolArmed());
  }

  /** A relation from a class to itself needs the mouse to leave the class and come back; a shaky click makes none. */
  @Test
  void loopOnlyAfterLeavingTheClass() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();
    Point2D inA = inClass(0);

    draw(inA, new Point2D.Double(inA.getX() + 2, inA.getY()));
    assertFalse(editing.canUndo());

    control.pressed(inA);
    control.dragged(new Point2D.Double(200, 200));
    control.dragged(inA);
    control.released(inA);

    assertEquals("11111111", relationOf(editing.source()).fromId);
    assertEquals("11111111", relationOf(editing.source()).toId);
  }

  /** While changes are tracked the new relation is marked as added. */
  @Test
  void drawnRelationTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(TWO_CLASSES);
    editing.toggleRelationTool();

    draw(inClass(0), inClass(1));

    assertEquals(ChangeType_V001.ADDED, relationOf(editing.source()).change.type);
    assertEquals("yellow", relationOf(editing.source()).change.changeSet);
  }

  /** A class without an ID gets one, because a relation refers to its classes by ID. */
  @Test
  void classWithoutIdGetsOne() throws ClassDiagramParseException {
    open("classdiagram {\n  class A at(10, 20)\n  class B (22222222) at(300, 20)\n}");
    editing.toggleRelationTool();

    draw(inClass(0), inClass(1));

    assertNotNull(new ClassDiagramParser_V001().parse(editing.source()).classes().get(0).id);
    assertEquals(new ClassDiagramParser_V001().parse(editing.source()).classes().get(0).id,
      relationOf(editing.source()).fromId);
  }

  /** While the tool is armed, a press doesn't select and a drag doesn't move the class. */
  @Test
  void armedToolDoesNotMoveClasses() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    editing.toggleRelationTool();
    Rectangle2D before = editing.scene().elementAreas.get(0).area;

    draw(inClass(0), new Point2D.Double(200, 200));

    assertEquals(before, editing.scene().elementAreas.get(0).area);
    assertFalse(editing.hasSelection());
  }
}
