package headless.saveload;

import headless.HeadlessExtension;
import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.view.AbstractSchrittView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

/** A file may use meta tag configurations which it doesn't have, because somebody changed it by hand or an agent did.
 * Like for pasted steps, the tags and stereotypes which refer to them are dropped, and the user is told. */
@ExtendWith(HeadlessExtension.class)
class UnknownMetaTagConfigLoadTest {

  private static final String SKIPPED_BRIEF = "msg.paste.tags.skipped.brief";
  private static final String BOTH_KINDS = "classdiagram {\n    class A (aaaaaaaa) <<:known>>\n    class B (bbbbbbbb) <<:gone>>\n}\n";

  private static File fileWithTagsAndDiagram() {
    return diagramm()
      .withMetaTagConfig("known", "Known", false)
      .withMainSequence(sequence()
        .withStep(simple("first").withTag("known").withTag("gone").withClassmanDiagram(BOTH_KINDS)))
      .persist();
  }

  private static ClassmanImageEditArea diagramOfFirstStep() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    return (ClassmanImageEditArea) step.getFirstEditArea().asTextArea().getParent().getEditAreaAt(1);
  }

  /** Scenario: The file has a tag and a stereotype of a configuration which the file doesn't have, besides ones of a
   * configuration it has. Only the unknown ones are dropped, and the user is told once. */
  @Test
  void testUnknownConfigsAreDroppedWithToast(@TempDir Path directory) throws IOException {
    new ModelCopyLoader(specman).loadModelCopy(fileWithTagsAndDiagram(), directory);

    assertEquals(Set.of("known"), specman.findStepByStepID("1").getTagConfigNames());
    String source = diagramOfFirstStep().getSource();
    assertTrue(source.contains("<<:known>>"), source);
    assertFalse(source.contains("gone"), source);
    assertEquals(List.of(i18n(SKIPPED_BRIEF)), specman.shownMessages());
  }

  /** Scenario: The file only uses configurations it has. Nothing is dropped, nothing is said. */
  @Test
  void testKnownConfigsNeedNoToast(@TempDir Path directory) throws IOException {
    File file = diagramm()
      .withMetaTagConfig("known", "Known", false)
      .withMainSequence(sequence()
        .withStep(simple("first").withTag("known")
          .withClassmanDiagram("classdiagram {\n    class A (aaaaaaaa) <<:known>>\n}\n")))
      .persist();

    new ModelCopyLoader(specman).loadModelCopy(file, directory);

    assertEquals(Set.of("known"), specman.findStepByStepID("1").getTagConfigNames());
    assertTrue(diagramOfFirstStep().getSource().contains("<<:known>>"));
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: A reference in the old text of a change mark isn't a use, so it stays and nothing is said. */
  @Test
  void testBackupOfChangeMarkIsLeftAlone(@TempDir Path directory) throws IOException {
    String source = "classdiagram {\n    class A (aaaaaaaa) <<Entity>> was(yellow, \":gone\")\n}\n";
    File file = diagramm()
      .withMainSequence(sequence().withStep(simple("first").withClassmanDiagram(source)))
      .persist();

    new ModelCopyLoader(specman).loadModelCopy(file, directory);

    assertTrue(diagramOfFirstStep().getSource().contains(":gone"));
    assertEquals(List.of(), specman.shownMessages());
  }
}
