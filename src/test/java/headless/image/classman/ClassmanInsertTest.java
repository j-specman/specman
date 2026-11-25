package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.classman.ClassmanImageEditArea;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.ops.AddImageSpecmanOp.EMPTY_CLASSMAN;

/** A new Classman diagram is inserted with the toolbar button: empty, with the diagram editor opening right away, or
 * with the source of a file. Only a file with source which the editor would accept as well is taken over. */
class ClassmanInsertTest extends AbstractClassmanTest {

  private static final String REFUSED_MESSAGE_KEY = "msg.classman.file.invalid";

  /** Scenario: By default, the button inserts an empty diagram behind the text, and the user is offered the choice. */
  @Test
  void testToolbarButtonInsertsEmptyDiagramBehindText() {
    EditContainer container = containerOfNewStep();

    ClassmanImageEditArea diagram = insertDiagramByToolbarButton(container);

    assertEquals(EMPTY_CLASSMAN, diagram.getSource());
    assertTrue(diagram.getFullSizeImage().getWidth() > 0, "Even an empty diagram must have an extent");
    assertTrue(diagram.getTotalScalePercent() > 0, "The diagram must be shown on screen");
    assertTrue(specman.classmanEmptyDiagramWasOffered(), "starting empty must be offered for a new diagram");
  }

  /** Scenario: The user works in the diagram editor which opened with the new diagram. The diagram stays when the
   * editor is left, and what the editor did is undone step by step before the diagram itself goes. */
  @Test
  void testDiagramStaysAfterTheDiagramEditorDidSomething() {
    EditContainer container = containerOfNewStep();
    ClassmanImageEditArea diagram = insertDiagramByToolbarButton(container);

    diagram.changeSource(TWO_CLASSES);
    diagram.firstEditClosed(EMPTY_CLASSMAN);

    assertTrue(hasDiagram(container), "The diagram must stay");
    assertEquals(TWO_CLASSES, diagram.getSource());

    specman.undo();
    assertTrue(hasDiagram(container), "The first undo takes back what the editor did");
    assertEquals(EMPTY_CLASSMAN, diagram.getSource());

    specman.undo();
    assertFalse(hasDiagram(container), "The second undo takes the diagram out");
  }

  /** Scenario: Leaving the diagram editor without having done anything takes the diagram out as if it had never been
   * inserted. */
  @Test
  void testLeavingTheEditorWithoutContentTakesTheDiagramOutWithoutTrace() {
    EditContainer container = containerOfNewStep();
    ClassmanImageEditArea diagram = insertDiagramByToolbarButton(container);

    diagram.firstEditClosed(EMPTY_CLASSMAN);

    assertFalse(hasDiagram(container), "The diagram must be gone");
    assertFalse(specman.getUndoManager().canRedo(), "The insertion must not be redoable");
    assertTrue(specman.getUndoManager().canUndo(), "The step created before must still be undoable");
  }

  /** Scenario: The user chooses a file with valid source. The diagram shows its content, and a single undo takes the
   * whole diagram out again. */
  @Test
  void testDiagramTakesTheSourceOfTheChosenFile(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, TWO_CLASSES));

    assertTrue(hasDiagram(container), "diagram must be inserted");
    assertEquals(TWO_CLASSES, ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource());
    assertEquals(List.of(), specman.shownMessages());

    specman.undo();

    assertFalse(hasDiagram(container), "a single undo must take the diagram out");
  }

  /** Scenario: Files from Windows editors have CRLF line breaks and possibly a byte order mark. The source gets the
   * line breaks Specman uses and the mark is dropped. */
  @Test
  void testWindowsLineBreaksAndByteOrderMarkAreNormalized(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    String windowsContent = (char) 0xFEFF + TWO_CLASSES.replace("\n", "\r\n");

    insertFrom(fileWith(directory, windowsContent));

    assertEquals(TWO_CLASSES, ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource());
  }

  /** Scenario: The file has a syntax error. No diagram is created, and the user gets told why. */
  @Test
  void testFileWithSyntaxErrorIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, "classdiagram { class }"));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(REFUSED_MESSAGE_KEY), specman.shownMessages());
  }

  /** Scenario: A file without any content is refused, because the diagram would be a tiny picture nobody finds. */
  @Test
  void testFileWithoutContentIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, "classdiagram { }"));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(REFUSED_MESSAGE_KEY), specman.shownMessages());
  }

  /** Scenario: The user cancels the dialog. Nothing is inserted. */
  @Test
  void testCancellingInsertsNothing() {
    EditContainer container = containerOfNewStep();

    insertFrom(SourceFileChoice.cancelled());

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(), specman.shownMessages());
  }

  private static SourceFileChoice fileWith(Path directory, String content) throws IOException {
    Path file = directory.resolve("diagram.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return SourceFileChoice.file(file.toFile());
  }

  private static void insertFrom(SourceFileChoice choice) {
    specman.answerClassmanFileChoiceWith(choice);
    specman.doEdit(net.atlanticbb.tantlinger.ui.text.actions.ClassmanAction.class);
    specman.layoutComponents();
  }
}
