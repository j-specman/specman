package classman.editor;

import classman.model.v001.NoteModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
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

class NoteCreationTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASS_ONLY = "classdiagram {\n  class A (11111111) at(10, 20)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private DiagramCanvas canvas;
  private TextEditor editor;

  private void open() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(CLASS_ONLY, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      canvas = new DiagramCanvas(editing.scene());
      editor = new TextEditor(canvas, editing, () -> canvas.setScene(editing.scene()));
    });
  }

  private List<NoteModel_V001> notes() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).notes();
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

  /** The new note is in the picture and selected, with an empty text, but not in the diagram yet. */
  @Test
  void noteWithoutText() throws Exception {
    open();

    SceneTextArea text = editing.newNote(300, 40);

    assertEquals("", editing.textOf(text));
    assertEquals(2, editing.scene().elementAreas.size());
    assertNotNull(editing.selectedNote());
    assertTrue(notes().isEmpty());
    assertFalse(editing.canUndo());
  }

  /** The text makes the note part of the diagram, where it was dropped, with an ID and no width, in one step. */
  @Test
  void textCreatesNote() throws Exception {
    open();

    editing.setText(editing.newNote(300, 40), "Remember this");

    assertEquals(1, notes().size());
    NoteModel_V001 note = notes().get(0);
    assertEquals("Remember this", note.text);
    assertNull(note.width);
    assertEquals(300, note.position.x);
    assertEquals(40, note.position.y);
    assertNotNull(note.id);
    assertNull(note.textChange);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** While changes are tracked, the new note is marked as added, and its text needs no mark of its own. */
  @Test
  void trackedNoteIsAdded() throws Exception {
    host.trackedChangeSet = "yellow";
    open();

    editing.setText(editing.newNote(300, 40), "Remember this");

    assertTrue(editing.source().contains("change(added, yellow)"), editing.source());
    assertFalse(editing.source().contains("was("), editing.source());
  }

  /** The empty text, or dropping the note, takes it away again, without an operation. */
  @Test
  void noteWithoutTextIsRemoved() throws Exception {
    open();

    editing.setText(editing.newNote(300, 40), "");
    assertEquals(1, editing.scene().elementAreas.size());
    assertFalse(editing.hasSelection());

    editing.newNote(300, 40);
    editing.discardNewPart();

    assertEquals(1, editing.scene().elementAreas.size());
    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** The field opens at once, empty, inside the box of the new note, and Return creates the note. */
  @Test
  void fieldCreatesNote() throws Exception {
    open();

    SwingUtilities.invokeAndWait(() -> editor.startNewNote(300, 40));

    assertTrue(editor.isEditing());
    assertEquals("", field().getText());
    java.awt.Rectangle box = canvas.toCanvas(editing.scene().elementAreas.get(1).area);
    assertTrue(field().getX() >= box.x && field().getX() + field().getWidth() <= box.x + box.width,
      "the field is inside the box");

    typeAndPressReturn("Remember this");

    assertFalse(editor.isEditing());
    assertEquals(List.of("Remember this"), notes().stream().map(note -> note.text).toList());
  }
}
