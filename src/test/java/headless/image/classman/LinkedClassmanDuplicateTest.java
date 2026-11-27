package headless.image.classman;

import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.classman.ClassmanLink;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.COPY_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.PASTE_ITEM;

/** A document links a file at most once, because two diagrams on one file would overwrite each other's changes when the
 * document is saved. */
class LinkedClassmanDuplicateTest extends AbstractClassmanTest {

  private static LinkedClassmanImageEditArea diagramOfStep(String stepId) {
    return (LinkedClassmanImageEditArea) containerOf(specman.findStepByStepID(stepId)).getEditAreaAt(1);
  }

  private static EditContainer containerOf(AbstractSchrittView step) {
    return step.getFirstEditArea().asTextArea().getParent();
  }

  private static void save(Path directory) {
    specman.setDiagrammDatei(directory.resolve("document.nsd").toFile());
    specman.diagrammSpeichern(false);
  }

  private static String content(File file) throws IOException {
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }

  /** Scenario: A document which links the same file twice is loaded. The first diagram is the one which writes the
   * file, the second never does, whatever the user changes. */
  @Test
  void testSecondDiagramOfFileLoadedTwiceIsDuplicate(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, TWO_CLASSES);
    String link = ClassmanLink.of(file, "", null, null);
    File nsd = diagramm().withMainSequence(sequence()
      .withStep(simple("first").withLinkedClassmanDiagram(link))
      .withStep(simple("second").withLinkedClassmanDiagram(link)))
      .persist();
    new ModelCopyLoader(specman).loadModelCopy(nsd, directory);

    assertFalse(diagramOfStep("1").isDuplicate());
    assertTrue(diagramOfStep("2").isDuplicate());
    diagramOfStep("2").changeSource(THREE_CLASSES);
    save(directory);

    assertEquals(TWO_CLASSES, content(file));
    diagramOfStep("1").changeSource(THREE_CLASSES);
    save(directory);
    assertEquals(THREE_CLASSES, content(file));
  }

  /** Scenario: The user links a file which another diagram of the document links already. Nothing is inserted, and the
   * user is told why. */
  @Test
  void testFileLinkedAlreadyIsRefused(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, TWO_CLASSES);
    containerOfNewStep();
    insertLinkedDiagram(file);
    EditContainer second = containerOfNewStep();

    insertLinkedDiagram(file);

    assertFalse(hasDiagram(second), "no diagram must be inserted");
    assertEquals(List.of("msg.classman.file.linked.already"), specman.shownMessages());
  }

  /** Scenario: The user copies a step with a linked diagram and pastes it into the same document. The pasted diagram is
   * the duplicate, the diagram it was copied from stays what it is. */
  @Test
  void testPastedDiagramIsDuplicate(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, TWO_CLASSES);
    EinfacherSchrittView original = specman.clickNewSimpleStep();
    specman.focus(original);
    insertLinkedDiagram(file);

    specman.clickContextMenuItem(original, COPY_ITEM);
    specman.clickContextMenuItem(original, PASTE_ITEM);

    assertFalse(diagramOfStep("1").isDuplicate());
    assertTrue(diagramOfStep("2").isDuplicate());
    diagramOfStep("2").changeSource(THREE_CLASSES);
    save(directory);
    assertEquals(TWO_CLASSES, content(file));
  }
}
