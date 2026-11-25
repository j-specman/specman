package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassCreationTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String EMPTY = "classdiagram {\n  class A (11111111) at(10, 20)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private DiagramCanvas canvas;
  private TextEditor editor;

  private void open() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(EMPTY, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      canvas = new DiagramCanvas(editing.scene());
      editor = new TextEditor(canvas, editing, () -> canvas.setScene(editing.scene()));
    });
  }

  private List<ClassModel_V001> classes() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes();
  }

  private JTextComponent field() {
    return (JTextComponent) canvas.getComponent(0);
  }

  private void typeAndPressReturn(String text) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      field().setText(text);
      field().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        .actionPerformed(new ActionEvent(field(), ActionEvent.ACTION_PERFORMED, ""));
    });
    SwingUtilities.invokeAndWait(() -> { });
  }

  /** The new class is in the picture and selected, with an empty name, but not in the diagram yet. */
  @Test
  void classWithoutName() throws Exception {
    open();

    SceneTextArea name = editing.newClass(300, 40);

    assertEquals("", editing.textOf(name));
    assertEquals(2, editing.scene().elementAreas.size());
    assertNotNull(editing.selectedClass());
    assertEquals(1, classes().size());
    assertFalse(editing.canUndo());
  }

  /** The name makes the class part of the diagram, at the place where it was dropped, in one step. */
  @Test
  void nameCreatesClass() throws Exception {
    open();

    editing.setText(editing.newClass(300, 40), "Book");

    assertEquals(2, classes().size());
    ClassModel_V001 book = classes().get(1);
    assertEquals("Book", book.name);
    assertNull(book.width);
    assertEquals(300, book.position.x);
    assertEquals(40, book.position.y);
    assertNotNull(book.id);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
    assertNotNull(editing.selectedClass());
  }

  /** While changes are tracked, the new class is marked as added. */
  @Test
  void trackedClassIsAdded() throws Exception {
    host.trackedChangeSet = "yellow";
    open();

    editing.setText(editing.newClass(300, 40), "Book");

    assertTrue(editing.source().contains("change(added, yellow)"), editing.source());
  }

  /** The empty name takes the class away again, without an operation, and nothing is selected. */
  @Test
  void emptyNameRemovesClass() throws Exception {
    open();

    editing.setText(editing.newClass(300, 40), "");

    assertEquals(1, editing.scene().elementAreas.size());
    assertFalse(editing.hasSelection());
    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** Dropping the class without a name, or doing something else, takes it away too. */
  @Test
  void droppedClassIsRemoved() throws Exception {
    open();
    editing.newClass(300, 40);
    editing.discardNewPart();
    assertEquals(1, editing.scene().elementAreas.size());

    editing.newClass(300, 40);
    editing.newClass(500, 40);
    assertEquals(2, editing.scene().elementAreas.size());

    editing.setText(editing.newClass(700, 40), "Page");
    assertEquals(2, classes().size());
  }

  /** The field opens at once, empty, over the name of the new class. */
  @Test
  void fieldOpensAtOnce() throws Exception {
    open();

    SwingUtilities.invokeAndWait(() -> editor.startNewClass(300, 40));

    assertTrue(editor.isEditing());
    assertEquals("", field().getText());
  }

  /** The box of the new class is wide enough for the field of its name. */
  @Test
  void fieldFitsIntoBox() throws Exception {
    open();

    SwingUtilities.invokeAndWait(() -> editor.startNewClass(300, 40));

    java.awt.Rectangle box = canvas.toCanvas(editing.scene().elementAreas.get(1).area);
    assertTrue(field().getX() >= box.x, "field starts inside the box");
    assertTrue(field().getX() + field().getWidth() <= box.x + box.width, "field ends inside the box");
  }

  /** Return with a name creates the class. */
  @Test
  void returnCreatesClass() throws Exception {
    open();
    SwingUtilities.invokeAndWait(() -> editor.startNewClass(300, 40));

    typeAndPressReturn("Book");

    assertFalse(editor.isEditing());
    assertEquals(List.of("A", "Book"), classes().stream().map(box -> box.name).toList());
  }

  /** Return with an empty name takes the class away again. */
  @Test
  void returnWithoutNameRemovesClass() throws Exception {
    open();
    SwingUtilities.invokeAndWait(() -> editor.startNewClass(300, 40));

    typeAndPressReturn("");

    assertFalse(editor.isEditing());
    assertEquals(1, editing.scene().elementAreas.size());
    assertFalse(editing.hasSelection());
    assertEquals(1, classes().size());
  }
}
