package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.metatag.generic.config.MetaTagConfig;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.COPY_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.PASTE_ITEM;

/** A diagram which comes from outside, pasted with its step or read from a file, may refer to meta tag configurations
 * which this document doesn't have. Unlike the tags at a pasted step, such stereotypes stay as they are: the diagram
 * shows them as unresolved, and nothing is said. */
class ClassmanForeignConfigTest extends AbstractClassmanTest {

  private static final MetaTagConfig DOMAIN = new MetaTagConfig("Domain", "Domain Value", null, null, null, null, true);
  private static final String USING_DOMAIN = "classdiagram {\n    class A (aaaaaaaa) <<:Domain>>\n    class B (bbbbbbbb) <<Entity>>\n}\n";

  private static ClassmanImageEditArea pastedDiagramOfCopiedStepWithoutConfig() {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    AbstractSchrittView original = copiedStepWithDiagram();
    EinfacherSchrittView target = specman.clickNewSimpleStep();
    specman.clickContextMenuItem(original, COPY_ITEM);
    specman.setMetaTagConfigs(List.of());
    specman.clickContextMenuItem(target, PASTE_ITEM);
    specman.layoutComponents();
    AbstractSchrittView pasted = specman.findStepByStepID("3");
    return (ClassmanImageEditArea) pasted.getFirstEditArea().asTextArea().getParent().getEditAreaAt(1);
  }

  private static AbstractSchrittView copiedStepWithDiagram() {
    diagramWithSource(containerOfNewStep(), USING_DOMAIN);
    return specman.findStepByStepID("1");
  }

  /** Scenario: The configuration is unknown in the document which the step is pasted into. The diagram comes as it
   * is. */
  @Test
  void testPastedStereotypeOfUnknownConfigStays() {
    ClassmanImageEditArea pasted = pastedDiagramOfCopiedStepWithoutConfig();

    assertEquals(USING_DOMAIN, pasted.getSource());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: Undoing the paste takes the step out, as for any pasted step. */
  @Test
  void testUndoTakesPastedStepOut() {
    pastedDiagramOfCopiedStepWithoutConfig();

    specman.undo();

    specman.assertStepNumbers(List.of("1", "2"));
  }

  /** Scenario: A file with a diagram which uses a configuration of another document is inserted as it is. */
  @Test
  void testFileWithUnknownConfigIsInsertedAsIs(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(directory, USING_DOMAIN);

    assertEquals(USING_DOMAIN, ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource());
    assertEquals(List.of(), specman.shownMessages());
  }

  private static void insertFrom(Path directory, String content) throws IOException {
    Path file = directory.resolve("diagram.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    specman.answerClassmanFileChoiceWith(SourceFileChoice.file(file.toFile()));
    specman.doEdit(net.atlanticbb.tantlinger.ui.text.actions.ClassmanAction.class);
    specman.layoutComponents();
  }
}
