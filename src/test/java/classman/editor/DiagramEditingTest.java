package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String EMPTY = "classdiagram {\n\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());

  /** The class lands where it was dropped and is reported with ID and name. */
  @Test
  void droppedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(120, 80);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertEquals("NewClass", box.name);
    assertEquals(120, box.position.x);
    assertEquals(80, box.position.y);
    assertTrue(box.id.matches("[0-9a-f]{8}"));
    assertEquals(List.of(editing.source()), host.reportedSources);
  }

  /** Several new classes can be told apart. */
  @Test
  void distinctClasses() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);
    editing.addClass(200, 0);
    editing.addClass(400, 0);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    Set<String> names = new HashSet<>();
    Set<String> ids = new HashSet<>();
    for (ClassModel_V001 box : boxes) {
      names.add(box.name);
      ids.add(box.id);
    }
    assertEquals(3, names.size());
    assertEquals(3, ids.size());
    assertEquals(3, host.reportedSources.size());
  }

  /** The picture follows the diagram after each operation. */
  @Test
  void sceneFollowsSource() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    double widthBefore = editing.scene().width;

    editing.addClass(300, 300);

    assertTrue(editing.scene().width > widthBefore);
  }

  /** While changes are tracked, a new class is marked as added by the change set. */
  @Test
  void trackedClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertNotNull(box.change);
    assertEquals(ChangeType_V001.ADDED, box.change.type);
    assertEquals("yellow", box.change.changeSet);
  }

  /** Without tracking, a new class carries no mark. */
  @Test
  void untrackedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    assertNull(onlyClassOf(editing.source()).change);
  }

  /** What the diagram already has stays when a class is added. */
  @Test
  void existingElementsKept() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.addClass(300, 20);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    assertEquals(2, boxes.size());
    assertEquals("Book", boxes.get(0).name);
    assertEquals("04de91aa", boxes.get(0).id);
  }

  /** A diagram which can't be read can't be edited. */
  @Test
  void unreadableSource() {
    assertThrows(ClassDiagramParseException.class, () -> new DiagramEditing("classdiagram {", host, texts));
  }

  /** A diagram which refers to a class it doesn't have can't be edited. */
  @Test
  void danglingReference() {
    String source = "classdiagram {\n  extends 11111111 -> 22222222\n}";
    assertThrows(IllegalArgumentException.class, () -> new DiagramEditing(source, host, texts));
  }

  private static ClassDiagramModel_V001 modelOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source);
  }

  private static ClassModel_V001 onlyClassOf(String source) throws ClassDiagramParseException {
    List<ClassModel_V001> boxes = modelOf(source).classes();
    assertEquals(1, boxes.size());
    return boxes.get(0);
  }
}
