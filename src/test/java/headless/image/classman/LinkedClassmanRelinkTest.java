package headless.image.classman;

import headless.HeadlessExtension;
import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.image.SourceImageContextMenu;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.LinkAliases;
import specman.link.LinkLocation;
import specman.link.classman.ClassmanLink;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;

import javax.swing.JMenuItem;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A diagram which names its file by an alias nobody knows, or which stands for a file that is gone, is repaired by
 * telling which file the alias stands for: a click on the placeholder asks for it. The link itself stays as it is. */
@ExtendWith(HeadlessExtension.class)
class LinkedClassmanRelinkTest {
  private static final String SOURCE = "classdiagram {\n    class A (aaaaaaaa)\n}\n";
  private static final String OTHER_SOURCE = "classdiagram {\n    class B (bbbbbbbb)\n}\n";
  private static final String CHANGED_SOURCE = "classdiagram {\n    class A (aaaaaaaa)\n    class C (cccccccc)\n}\n";

  private static File diagramFile(Path directory, String name, String content) throws IOException {
    Path file = directory.resolve(name);
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  /** Loads a document with one step for each link, which has the diagram of that link. */
  private static void load(Path directory, String... links) throws IOException {
    var sequence = sequence();
    for (String link : links) {
      sequence = sequence.withStep(simple("step").withLinkedClassmanDiagram(link));
    }
    File nsd = diagramm().withMainSequence(sequence).persist();
    new ModelCopyLoader(specman).loadModelCopy(nsd, directory);
  }

  private static LinkedClassmanImageEditArea diagramOfStep(String stepId) {
    return (LinkedClassmanImageEditArea) specman.findStepByStepID(stepId).getFirstEditArea().asTextArea().getParent()
      .getEditAreaAt(1);
  }

  /** Scenario: Nobody knows the alias. The user clicks the placeholder and chooses the file. The diagram of the file is
   * shown, the alias stands for the file from now on, and the link in the model is what it was. */
  @Test
  void testUnknownAliasIsAssignedToChosenFile(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://hilfe");
    specman.answerFileChoicesWith(file);

    specman.leftClick(diagramOfStep("1"));

    LinkedClassmanImageEditArea diagram = diagramOfStep("1");
    assertFalse(diagram.isPlaceholder());
    assertEquals(SOURCE, diagram.getSource());
    assertEquals("hilfe", LinkAliases.aliasOf(file));
    assertEquals("classman://hilfe", ((LinkedClassmanImageEditAreaModel_V002) diagram.toModel(false)).link);
    assertFalse(diagram.needsSaving(), "reading the file is no change of the user");
    assertEquals(List.of("hilfe"), specman.aliasesAskedFor());
  }

  /** Scenario: The alias stands for a file which was moved. The user chooses the file where it is now. The alias is
   * taken away from the old file, which counts as having none. */
  @Test
  void testAliasOfMissingFileMovesToChosenFile(@TempDir Path directory) throws IOException {
    File gone = directory.resolve("gone.cld").toFile();
    LinkAliases.assign(gone, "hilfe");
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://hilfe");
    specman.answerFileChoicesWith(file);

    specman.leftClick(diagramOfStep("1"));

    assertFalse(diagramOfStep("1").isPlaceholder());
    assertEquals("hilfe", LinkAliases.aliasOf(file));
    assertEquals("", LinkAliases.aliasOf(gone));
  }

  /** Scenario: The user cancels the choice of the file. Nothing changes. */
  @Test
  void testCancellingLeavesPlaceholder(@TempDir Path directory) throws IOException {
    load(directory, "classman://hilfe");
    specman.answerFileChoicesWith(null);

    specman.leftClick(diagramOfStep("1"));

    assertTrue(diagramOfStep("1").isPlaceholder());
    assertEquals(List.of("hilfe"), specman.aliasesAskedFor());
  }

  /** Scenario: The user chooses a file which has no class diagram. The user is told, and nothing is assigned. */
  @Test
  void testFileWithoutDiagramIsRefused(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", "classdiagram { class }");
    load(directory, "classman://hilfe");
    specman.answerFileChoicesWith(file);

    specman.leftClick(diagramOfStep("1"));

    assertTrue(diagramOfStep("1").isPlaceholder());
    assertFalse(LinkAliases.isAssigned(file));
    assertEquals(List.of("msg.classman.file.invalid"), specman.shownMessages());
  }

  /** Scenario: The user chooses a file which another diagram of the document links. It is refused, as it would be for a
   * new diagram, and nothing is assigned. */
  @Test
  void testFileLinkedByAnotherDiagramIsRefused(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://nobody", ClassmanLink.of(file, "", null, null));
    specman.answerFileChoicesWith(file);

    specman.leftClick(diagramOfStep("1"));

    assertTrue(diagramOfStep("1").isPlaceholder());
    assertFalse(LinkAliases.isAssigned(file));
    assertEquals(List.of("msg.classman.file.linked.already"), specman.shownMessages());
  }

  /** Scenario: The link names the file by its path, which leads nowhere. There is no alias to say a file for, so the user
   * chooses the file and how the link names it, and the link is replaced. The diagram of the file is shown, the tooltip
   * has the new link, and the diagram has nothing to write. */
  @Test
  void testLinkByPathIsReplacedByNewLink(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://./gone.cld");
    specman.answerFileChoicesWith(file);
    specman.answerClassmanLocationWith(LinkLocation.relative(file, directory.toFile()));

    specman.leftClick(diagramOfStep("1"));

    LinkedClassmanImageEditArea diagram = diagramOfStep("1");
    assertFalse(diagram.isPlaceholder());
    assertEquals(SOURCE, diagram.getSource());
    assertEquals("classman://./hilfe.cld", diagram.getLink());
    assertEquals("classman://./hilfe.cld", diagram.getToolTipText());
    assertFalse(diagram.needsSaving());
    assertEquals(List.of(), specman.aliasesAskedFor());
  }

  /** Scenario: One undo brings back the placeholder with the old link, a redo the diagram. */
  @Test
  void testNewLinkIsOneUndoStep(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://./gone.cld");
    specman.answerFileChoicesWith(file);
    specman.answerClassmanLocationWith(LinkLocation.relative(file, directory.toFile()));
    specman.leftClick(diagramOfStep("1"));

    specman.undo();

    assertTrue(diagramOfStep("1").isPlaceholder());
    assertEquals("classman://./gone.cld", diagramOfStep("1").getLink());
    specman.redo();
    assertFalse(diagramOfStep("1").isPlaceholder());
    assertEquals("classman://./hilfe.cld", diagramOfStep("1").getLink());
  }

  /** Scenario: The diagram is a duplicate, whose alias is fine. The user chooses another file and an alias for it. The
   * alias of the first diagram stands for its file as before, and one undo makes the diagram a duplicate again. */
  @Test
  void testDuplicateGetsLinkToOtherFile(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    File other = diagramFile(directory, "other.cld", "classdiagram {\n    class B (bbbbbbbb)\n}\n");
    LinkAliases.assign(file, "hilfe");
    load(directory, "classman://hilfe", "classman://hilfe");
    specman.answerFileChoicesWith(other);
    specman.answerClassmanLocationWith(LinkLocation.alias("other"));

    specman.leftClick(diagramOfStep("2"));

    assertFalse(diagramOfStep("2").isPlaceholder());
    assertEquals("classman://other", diagramOfStep("2").getLink());
    assertEquals("other", LinkAliases.aliasOf(other));
    assertEquals("hilfe", LinkAliases.aliasOf(file));
    assertEquals(SOURCE, diagramOfStep("1").getSource());
    specman.undo();
    assertTrue(diagramOfStep("2").isDuplicate());
    assertEquals("classman://hilfe", diagramOfStep("2").getLink());
  }

  /** Scenario: The user cancels the question how the link names the file. Nothing changes, and nothing is decided about
   * the file. */
  @Test
  void testCancellingTheLocationDialogChangesNothing(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, "classman://./gone.cld");
    specman.answerFileChoicesWith(file);
    specman.answerClassmanLocationWith(null);

    specman.leftClick(diagramOfStep("1"));

    assertTrue(diagramOfStep("1").isPlaceholder());
    assertEquals("classman://./gone.cld", diagramOfStep("1").getLink());
    assertFalse(LinkAliases.isAssigned(file));
    assertEquals(1, specman.classmanFilesAskedFor());
  }

  /** Scenario: The menu of a diagram which shows its file offers relinking instead of importing, and editing stays
   * possible. */
  @Test
  void testMenuOfLinkedDiagramOffersRelinkInsteadOfImport(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, ClassmanLink.of(file, "", null, null));
    LinkedClassmanImageEditArea diagram = diagramOfStep("1");

    JMenuItem edit = specman.contextMenuItem(diagram, SourceImageContextMenu.EDIT_ITEM);
    JMenuItem relink = specman.contextMenuItem(diagram, SourceImageContextMenu.RELINK_ITEM);

    assertTrue(edit.isEnabled());
    assertTrue(relink.isEnabled());
    assertThrows(IllegalStateException.class, () -> specman.contextMenuItem(diagram, SourceImageContextMenu.IMPORT_ITEM));
  }

  /** Scenario: A placeholder can't be edited, and can be relinked. A diagram of a file which has no class diagram can't
   * be edited either, as nothing of it would reach the file. */
  @Test
  void testMenuOfDiagramWhichIsNotShownDoesNotOfferEditing(@TempDir Path directory) throws IOException {
    diagramFile(directory, "invalid.cld", "classdiagram { class }");
    load(directory, "classman://nobody", "classman://./invalid.cld");

    for (String stepId : List.of("1", "2")) {
      LinkedClassmanImageEditArea diagram = diagramOfStep(stepId);

      assertFalse(specman.contextMenuItem(diagram, SourceImageContextMenu.EDIT_ITEM).isEnabled());
      assertTrue(specman.contextMenuItem(diagram, SourceImageContextMenu.RELINK_ITEM).isEnabled());
    }
  }

  /** Scenario: The user relinks a diagram which works to another file, from the menu. The diagram shows the other file,
   * and the changes the user hadn't written yet are not lost: one undo brings back the link and them. */
  @Test
  void testRelinkFromMenuKeepsUnwrittenChangesForUndo(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    File other = diagramFile(directory, "other.cld", OTHER_SOURCE);
    String link = ClassmanLink.of(file, "", null, null);
    load(directory, link);
    LinkedClassmanImageEditArea diagram = diagramOfStep("1");
    diagram.changeSource(CHANGED_SOURCE);
    specman.answerFileChoicesWith(other);
    specman.answerClassmanLocationWith(LinkLocation.alias("other"));

    specman.clickContextMenuItem(diagram, SourceImageContextMenu.RELINK_ITEM);

    assertEquals(OTHER_SOURCE, diagram.getSource());
    assertEquals("classman://other", diagram.getLink());
    assertFalse(diagram.needsSaving());
    specman.undo();
    assertEquals(CHANGED_SOURCE, diagram.getSource());
    assertEquals(link, diagram.getLink());
    assertTrue(diagram.needsSaving());
  }

  /** Scenario: The user relinks a diagram to the file it has already, only to name it another way. That is no
   * duplicate. */
  @Test
  void testRelinkToOwnFileWithOtherLocation(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, "hilfe.cld", SOURCE);
    load(directory, ClassmanLink.of(file, "", null, null));
    LinkedClassmanImageEditArea diagram = diagramOfStep("1");
    specman.answerFileChoicesWith(file);
    specman.answerClassmanLocationWith(LinkLocation.alias("hilfe"));

    specman.clickContextMenuItem(diagram, SourceImageContextMenu.RELINK_ITEM);

    assertEquals("classman://hilfe", diagram.getLink());
    assertFalse(diagram.isPlaceholder());
    assertEquals(List.of(), specman.shownMessages());
  }
}
