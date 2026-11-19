package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.SourceImageContextMenu;
import specman.editarea.image.classman.ClassmanImageEditArea;

import javax.swing.JMenuItem;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;

/** The context menu of a Classman diagram. Its item to edit opens the editor next to the diagram, which needs a
 * diagram on screen and is therefore not covered here. */
class ClassmanContextMenuTest extends AbstractClassmanTest {

  /** Scenario: A right click on a diagram offers to edit, import into and delete it. */
  @Test
  void testMenuOffersEditImportAndDelete() {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());

    JMenuItem edit = specman.contextMenuItem(diagram, SourceImageContextMenu.EDIT_ITEM);
    JMenuItem importSource = specman.contextMenuItem(diagram, SourceImageContextMenu.IMPORT_ITEM);
    JMenuItem delete = specman.contextMenuItem(diagram, SourceImageContextMenu.DELETE_ITEM);

    assertTrue(edit.isEnabled() && edit.isVisible(), "edit must be available");
    assertTrue(importSource.isEnabled() && importSource.isVisible(), "import must be available");
    assertTrue(delete.isEnabled() && delete.isVisible(), "delete must be available");
  }

  /** Scenario: Deleting from the menu takes the diagram out. Undo brings it back. */
  @Test
  void testDeleteRemovesTheDiagramAndCanBeUndone() {
    EditContainer container = containerOfNewStep();
    ClassmanImageEditArea diagram = existingDiagram(container);

    specman.clickContextMenuItem(diagram, SourceImageContextMenu.DELETE_ITEM);

    assertFalse(hasDiagram(container), "the diagram must be gone");

    specman.undo();

    assertTrue(hasDiagram(container), "undo must bring the diagram back");
  }

  /** Scenario: In change mode, deleting a diagram which existed before only marks it as deleted. Rejecting the
   * changes takes the deletion back. */
  @Test
  void testDeleteInChangeModeMarksTheDiagramAsDeleted() {
    EditContainer container = containerOfNewStep();
    ClassmanImageEditArea diagram = existingDiagram(container);
    specman.toggleChangeMode();
    specman.selectChangeSet(ChangeSet.ALL.get("blue"));

    specman.clickContextMenuItem(diagram, SourceImageContextMenu.DELETE_ITEM);

    assertTrue(hasDiagram(container), "the diagram must stay as deleted");
    assertTrue(diagram.getChangeInfo().isDeleted(), "the diagram must be marked as deleted");

    specman.click(REJECT_CHANGES_BUTTON);

    assertTrue(diagram.getChangeInfo().isUntracked(), "the deletion must be taken back");
  }

  /** Scenario: Importing replaces the source of the diagram by the content of the chosen file. Undo brings the
   * source from before back. */
  @Test
  void testImportReplacesTheSourceAndCanBeUndone(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());

    importFile(diagram, directory, THREE_CLASSES);

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertEquals(List.of(), specman.shownMessages());

    specman.undo();

    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: In change mode, the import is an ordinary source change: Nothing is tracked about it, rejecting the
   * changes leaves the imported source as it is. */
  @Test
  void testImportInChangeModeIsNotTracked(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    specman.toggleChangeMode();
    specman.selectChangeSet(ChangeSet.ALL.get("blue"));

    importFile(diagram, directory, THREE_CLASSES);
    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(THREE_CLASSES, diagram.getSource());
  }

  /** Scenario: A file with a source which can't be drawn is refused with a message, and the diagram stays as it is. */
  @Test
  void testFileWithInvalidSourceIsRefused(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());

    importFile(diagram, directory, "classdiagram { class }");

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }

  private static void importFile(ClassmanImageEditArea diagram, Path directory, String content) throws IOException {
    Path file = directory.resolve("import.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    specman.answerClassmanFileChoiceWith(SourceFileChoice.file(file.toFile()));
    specman.clickContextMenuItem(diagram, SourceImageContextMenu.IMPORT_ITEM);
  }
}
