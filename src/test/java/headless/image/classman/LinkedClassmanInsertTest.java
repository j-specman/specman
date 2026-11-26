package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.LinkAliases;
import specman.link.classman.ClassmanLink;
import specman.link.classman.ClassmanLinkReference;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;

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

/** The user chooses a file in the dialog for a new diagram and says that the diagram is to link to it, not to take its
 * content. The diagram shows the content of the file, and the model gets the link. */
class LinkedClassmanInsertTest extends AbstractClassmanTest {
  private static final String NAMED_DIAGRAM = "classdiagram \"Hilfe\" (0a1b2c3d) {\n    class A (aaaaaaaa)\n}\n";

  private static File fileWith(Path directory, String content) throws IOException {
    Path file = directory.resolve("hilfe.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  private static void insertLinked(File file) {
    specman.answerClassmanFileChoiceWith(SourceFileChoice.linkedFile(file));
    specman.doEdit(net.atlanticbb.tantlinger.ui.text.actions.ClassmanAction.class);
    specman.layoutComponents();
  }

  private static String linkOf(EditContainer container) {
    LinkedClassmanImageEditArea diagram = (LinkedClassmanImageEditArea) container.getEditAreaAt(1);
    return ((LinkedClassmanImageEditAreaModel_V002) diagram.toModel(false)).link;
  }

  /** Scenario: The file has no alias. The diagram shows the content of the file and links it by its path. */
  @Test
  void testLinkedFileWithoutAliasIsLinkedByPath(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = fileWith(directory, TWO_CLASSES);

    insertLinked(file);

    assertTrue(container.getEditAreaAt(1) instanceof LinkedClassmanImageEditArea);
    assertEquals(TWO_CLASSES, ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource());
    ClassmanLinkReference reference = ClassmanLink.parse(linkOf(container));
    assertEquals(file.getAbsoluteFile(), reference.file());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user has an alias for the file. The link uses it. */
  @Test
  void testLinkedFileWithAliasIsLinkedByAlias(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = fileWith(directory, TWO_CLASSES);
    LinkAliases.assign(file, "hilfe");

    insertLinked(file);

    assertEquals("classman://hilfe", linkOf(container));
  }

  /** Scenario: The diagram has an ID and a name. The link tells them, so that it can be read by people. */
  @Test
  void testLinkTellsIdAndNameOfTheDiagram(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = fileWith(directory, NAMED_DIAGRAM);
    LinkAliases.assign(file, "hilfe");

    insertLinked(file);

    assertEquals("classman://hilfe,0a1b2c3d,Hilfe", linkOf(container));
  }

  /** Scenario: A single undo takes the linked diagram out again. */
  @Test
  void testUndoTakesLinkedDiagramOut(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertLinked(fileWith(directory, TWO_CLASSES));
    specman.undo();

    assertFalse(hasDiagram(container), "a single undo must take the diagram out");
  }

  /** Scenario: The file has no class diagram. Nothing is linked, and the user is told why. */
  @Test
  void testFileWithoutDiagramIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertLinked(fileWith(directory, "classdiagram { class }"));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }

  /** Scenario: Only a file with the extension of diagram files can be linked. */
  @Test
  void testFileOfOtherKindIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    Path file = directory.resolve("hilfe.txt");
    Files.writeString(file, TWO_CLASSES, StandardCharsets.UTF_8);

    insertLinked(file.toFile());

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }
}
