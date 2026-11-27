package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A linked diagram which the user changed is written to its file when the document is saved, before the model file.
 * The model file only has the link. */
class LinkedClassmanSaveTest extends AbstractClassmanTest {

  private static File save(Path directory) {
    File nsd = directory.resolve("document.nsd").toFile();
    specman.setDiagrammDatei(nsd);
    specman.diagrammSpeichern(false);
    return nsd;
  }

  /** Scenario: The user changes a linked diagram and saves. The file has the change, the model file has no source. */
  @Test
  void testChangedDiagramIsWrittenToItsFile(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);
    insertLinkedDiagram(file);

    ((ClassmanImageEditArea) container.getEditAreaAt(1)).changeSource(THREE_CLASSES);
    File nsd = save(directory);

    assertEquals(THREE_CLASSES, Files.readString(file.toPath(), StandardCharsets.UTF_8));
    String model = Files.readString(nsd.toPath(), StandardCharsets.UTF_8);
    assertTrue(model.contains("link=`"), model);
    assertFalse(model.contains("Universe"), model);
  }

  /** Scenario: The saved document is loaded again, and shows the change. */
  @Test
  void testSavedChangeIsThereAfterLoading(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    insertLinkedDiagram(diagramFile(directory, TWO_CLASSES));
    ((ClassmanImageEditArea) container.getEditAreaAt(1)).changeSource(THREE_CLASSES);
    File nsd = save(directory);

    specman.diagrammLaden(nsd);

    EditContainer reloaded = specman.findStepByStepID("1").getFirstEditArea().asTextArea().getParent();
    assertEquals(THREE_CLASSES, ((ClassmanImageEditArea) reloaded.getEditAreaAt(1)).getSource());
  }

  /** Scenario: The user saves without having changed the diagram. The file is not touched, not even to give it the
   * line breaks Specman uses. */
  @Test
  void testUnchangedFileIsNotTouched(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES.replace("\n", "\r\n"));
    byte[] before = Files.readAllBytes(file.toPath());
    insertLinkedDiagram(file);

    save(directory);

    assertArrayEquals(before, Files.readAllBytes(file.toPath()));
  }

  /** Scenario: The file can't be written. The user is told, and the model file isn't written either, so nothing gets
   * out of step. */
  @Test
  void testWriteErrorLeavesModelFileUnwritten(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);
    insertLinkedDiagram(file);
    ((ClassmanImageEditArea) container.getEditAreaAt(1)).changeSource(THREE_CLASSES);
    assertTrue(file.setReadOnly());
    try {
      File nsd = save(directory);

      assertFalse(nsd.exists(), "the model file must not be written");
      assertFalse(specman.shownMessages().isEmpty(), "the user must be told");
      assertEquals(TWO_CLASSES, Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }
    finally {
      assertTrue(file.setWritable(true));
    }
  }
}
