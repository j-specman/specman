package headless.image.classman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.LinkAliases;
import specman.link.LinkLocation;
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

  private static String linkOf(EditContainer container) {
    LinkedClassmanImageEditArea diagram = (LinkedClassmanImageEditArea) container.getEditAreaAt(1);
    return ((LinkedClassmanImageEditAreaModel_V002) diagram.toModel(false)).link;
  }

  /** Scenario: The file has no alias yet, and the user is asked how the link names it. The user chooses the absolute
   * path. The diagram shows the content of the file and links it by its path. */
  @Test
  void testLinkedFileWithoutAliasIsLinkedByPath(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);

    insertLinkedDiagram(file, LinkLocation.absolute(file));

    assertTrue(container.getEditAreaAt(1) instanceof LinkedClassmanImageEditArea);
    assertEquals(TWO_CLASSES, ((ClassmanImageEditArea) container.getEditAreaAt(1)).getSource());
    ClassmanLinkReference reference = ClassmanLink.parse(linkOf(container));
    assertEquals(file.getAbsoluteFile(), reference.location().resolve(new File(".")));
    assertEquals(List.of(file), specman.filesAskedClassmanLocationFor());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user chooses the path relative to the document. The link has it, and the file counts as decided
   * against an alias, so the user is not asked again. */
  @Test
  void testRelativePathFromTheDialogIsUsed(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);

    insertLinkedDiagram(file, LinkLocation.relative(file, directory.toFile()));

    assertEquals("classman://./hilfe.cld", linkOf(container));
    assertTrue(LinkAliases.isAssigned(file));
    assertEquals("", LinkAliases.aliasOf(file));
  }

  /** Scenario: The user gives an alias for the file. The link uses it, and the file has it from now on. */
  @Test
  void testAliasFromTheDialogIsUsedAndRemembered(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);

    insertLinkedDiagram(file, LinkLocation.alias("hilfe"));

    assertEquals("classman://hilfe", linkOf(container));
    assertEquals("hilfe", LinkAliases.aliasOf(file));
  }

  /** Scenario: The user cancels the question how the link names the file. Nothing is inserted and nothing is decided
   * about the file. */
  @Test
  void testCancellingTheAliasDialogCancelsTheInsertion(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);

    insertLinkedDiagram(file, null);

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertFalse(LinkAliases.isAssigned(file), "nothing must be decided about the file");
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The file has an alias already. The user is asked all the same, and chooses the relative path this time.
   * The link has it, and the alias of the file stays. */
  @Test
  void testFileWithAliasIsAskedAgainAndKeepsItsAlias(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, TWO_CLASSES);
    LinkAliases.assign(file, "hilfe");

    insertLinkedDiagram(file, LinkLocation.relative(file, directory.toFile()));

    assertEquals("classman://./hilfe.cld", linkOf(container));
    assertEquals(List.of(file), specman.filesAskedClassmanLocationFor());
    assertEquals("hilfe", LinkAliases.aliasOf(file));
  }

  /** Scenario: The diagram has an ID and a name. The link tells them, so that it can be read by people. */
  @Test
  void testLinkTellsIdAndNameOfTheDiagram(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    File file = diagramFile(directory, NAMED_DIAGRAM);

    insertLinkedDiagram(file, LinkLocation.alias("hilfe"));

    assertEquals("classman://hilfe#0a1b2c3d,Hilfe", linkOf(container));
  }

  /** Scenario: A single undo takes the linked diagram out again. */
  @Test
  void testUndoTakesLinkedDiagramOut(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertLinkedDiagram(diagramFile(directory, TWO_CLASSES));
    specman.undo();

    assertFalse(hasDiagram(container), "a single undo must take the diagram out");
  }

  /** Scenario: The file has no class diagram. Nothing is linked, and the user is told why. */
  @Test
  void testFileWithoutDiagramIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertLinkedDiagram(diagramFile(directory, "classdiagram { class }"));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }

  /** Scenario: Only a file with the extension of diagram files can be linked. */
  @Test
  void testFileOfOtherKindIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    Path file = directory.resolve("hilfe.txt");
    Files.writeString(file, TWO_CLASSES, StandardCharsets.UTF_8);

    insertLinkedDiagram(file.toFile());

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }
}
