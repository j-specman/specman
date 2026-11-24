package classman.editor;

import classman.model.v001.Compartment_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterfaceCreationTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private final RecordingEditorHost host = new RecordingEditorHost();

  private ClassModel_V001 onlyBox(DiagramEditing editing) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  private static Point2D centerOf(DiagramEditing editing, int index) {
    Rectangle2D area = editing.scene().elementAreas.get(index).area;
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  /** An interface is created like a class: dropped, named, with an ID, one undo step. */
  @Test
  void interfaceCreated() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing("classdiagram {\n}", host);

    editing.setText(editing.newClass(300, 40, ClassKind_V001.INTERFACE), "Runnable");

    ClassModel_V001 box = onlyBox(editing);
    assertEquals(ClassKind_V001.INTERFACE, box.kind);
    assertEquals("Runnable", box.name);
    assertNotNull(box.id);
    assertTrue(editing.canUndo());
    assertTrue(editing.source().contains("interface Runnable"));
  }

  /** While changes are tracked the new interface is marked as added. */
  @Test
  void interfaceCreatedTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing("classdiagram {\n}", host);

    editing.setText(editing.newClass(300, 40, ClassKind_V001.INTERFACE), "Runnable");

    assertEquals("yellow", onlyBox(editing).change.changeSet);
  }

  /** Dropped and given no name, the interface is gone again. */
  @Test
  void interfaceWithoutNameDiscarded() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing("classdiagram {\n}", host);

    editing.setText(editing.newClass(300, 40, ClassKind_V001.INTERFACE), "");

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).classes().isEmpty());
  }

  /** An interface can get a stereotype and tagged values, but no attribute. */
  @Test
  void interfaceHasNoAttributes() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing("classdiagram {\n  interface I (11111111) at(10, 10)\n"
      + "  class C (22222222) at(300, 10)\n}", host);
    editing.selectAt(centerOf(editing, 0));
    ClassModel_V001 anInterface = editing.selectedClass();
    editing.selectAt(centerOf(editing, 1));
    ClassModel_V001 aClass = editing.selectedClass();

    assertFalse(editing.canAddMemberTo(anInterface, Compartment_V001.ATTRIBUTES).can());
    assertTrue(editing.canAddMemberTo(aClass, Compartment_V001.ATTRIBUTES).can());
  }

  /** An enum is created like a class. Its values are lines without a type: a colon is not allowed, an info is. */
  @Test
  void enumValues() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing("classdiagram {\n}", host);
    editing.setText(editing.newClass(300, 40, ClassKind_V001.ENUM), "Color");
    editing.selectAt(centerOf(editing, 0));
    ClassModel_V001 box = editing.selectedClass();
    assertTrue(editing.canAddMemberTo(box, Compartment_V001.ATTRIBUTES).can());

    SceneTextArea line = editing.newMemberAtEnd(box, Compartment_V001.ATTRIBUTES);

    assertFalse(editing.accepts(line, "RED : int"));
    assertTrue(editing.accepts(line, "RED [primary]"));
    editing.setText(line, "RED [primary]");
    assertEquals(ClassKind_V001.ENUM, onlyBox(editing).kind);
    assertEquals("RED", onlyBox(editing).attributes.get(0).name);
    assertEquals("primary", onlyBox(editing).attributes.get(0).infos.get(0).text);
  }
}
