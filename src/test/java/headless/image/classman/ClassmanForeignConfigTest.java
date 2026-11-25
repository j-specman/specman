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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.COPY_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.PASTE_ITEM;

/** A diagram which comes from outside, pasted with its step or read from a file, may refer to meta tag configurations
 * which this document doesn't have. Like the tags at a pasted step, such stereotypes are dropped, and the user is told
 * which configurations were missing. */
class ClassmanForeignConfigTest extends AbstractClassmanTest {

  private static final MetaTagConfig DOMAIN = new MetaTagConfig("Domain", "Domain Value", null, null, null, null, true);
  private static final String USING_DOMAIN = "classdiagram {\n    class A (aaaaaaaa) <<@Domain>>\n    class B (bbbbbbbb) <<Entity>>\n}\n";
  private static final String SKIPPED_BRIEF = "msg.paste.tags.skipped.brief";

  private static ClassmanImageEditArea pastedDiagramOfCopiedStep(boolean configStillThere) {
    specman.setMetaTagConfigs(List.of(DOMAIN));
    AbstractSchrittView original = copiedStepWithDiagram();
    EinfacherSchrittView target = specman.clickNewSimpleStep();
    specman.clickContextMenuItem(original, COPY_ITEM);
    if (!configStillThere) {
      specman.setMetaTagConfigs(List.of());
    }
    specman.clickContextMenuItem(target, PASTE_ITEM);
    specman.layoutComponents();
    AbstractSchrittView pasted = specman.findStepByStepID("3");
    return (ClassmanImageEditArea) pasted.getFirstEditArea().asTextArea().getParent().getEditAreaAt(1);
  }

  private static AbstractSchrittView copiedStepWithDiagram() {
    diagramWithSource(containerOfNewStep(), USING_DOMAIN);
    return specman.findStepByStepID("1");
  }

  /** Scenario: The configuration is known in the document which the step is pasted into. The stereotype stays. */
  @Test
  void testPastedStereotypeOfKnownConfigStays() {
    ClassmanImageEditArea pasted = pastedDiagramOfCopiedStep(true);

    assertEquals(USING_DOMAIN, pasted.getSource());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The configuration is unknown. The pasted diagram loses that stereotype only, the user is told, and the
   * diagram which was copied is not touched. */
  @Test
  void testPastedStereotypeOfUnknownConfigIsDropped() {
    ClassmanImageEditArea pasted = pastedDiagramOfCopiedStep(false);

    assertFalse(pasted.getSource().contains("@Domain"), pasted.getSource());
    assertTrue(pasted.getSource().contains("<<Entity>>"), pasted.getSource());
    assertTrue(specman.shownMessages().contains(i18n(SKIPPED_BRIEF)), specman.shownMessages().toString());
    ClassmanImageEditArea original = (ClassmanImageEditArea) specman.findStepByStepID("1").getFirstEditArea()
      .asTextArea().getParent().getEditAreaAt(1);
    assertEquals(USING_DOMAIN, original.getSource());
  }

  /** Scenario: Undoing the paste takes the step out, as for any pasted step. */
  @Test
  void testUndoTakesPastedStepOut() {
    pastedDiagramOfCopiedStep(false);

    specman.undo();

    specman.assertStepNumbers(List.of("1", "2"));
  }

  /** Scenario: A file with a diagram which uses a configuration of another document is inserted: the diagram comes
   * without that stereotype, and the user is told. */
  @Test
  void testFileWithUnknownConfigIsInsertedWithoutIt(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(directory, USING_DOMAIN);

    String source = ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource();
    assertFalse(source.contains("@Domain"), source);
    assertTrue(source.contains("<<Entity>>"), source);
    assertTrue(specman.shownMessages().contains(i18n(SKIPPED_BRIEF)), specman.shownMessages().toString());
  }

  /** Scenario: The same file in a document which has the configuration: nothing is dropped. */
  @Test
  void testFileWithKnownConfigIsInsertedAsIs(@TempDir Path directory) throws IOException {
    specman.setMetaTagConfigs(List.of(DOMAIN));
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
