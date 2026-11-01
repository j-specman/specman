package headless.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.plantuml.PlantUmlContextMenu;
import specman.editarea.plantuml.PlantUmlFileChoice;
import specman.editarea.plantuml.PlantUmlImageEditArea;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;

/** Importing a file from the diagram's context menu replaces the source of an existing diagram. The file is subject to
 * the same rules as the one for a new diagram. */
class PlantUmlSourceImportTest extends AbstractPlantUmlTest {

  /** Scenario: The user imports a file with valid source. The diagram shows the content of the file instead of its
   * former source, and a single undo brings the former source back. The user is not offered an empty diagram, which
   * makes sense for a new diagram only. */
  @Test
  void testImportReplacesTheSourceAndCanBeUndone(@TempDir Path directory) throws IOException {
    PlantUmlImageEditArea diagram = existingDiagram();

    importFrom(diagram, fileWith(directory, THREE_CLASSES));

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertFalse(specman.plantUmlEmptyDiagramWasOffered(), "an empty diagram makes no sense for an import");
    assertEquals(List.of(), specman.shownMessages());

    specman.undo();

    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: The file has a syntax error. The source of the diagram stays as it was, and the user gets told why. */
  @Test
  void testFileWithSyntaxErrorIsRefused(@TempDir Path directory) throws IOException {
    PlantUmlImageEditArea diagram = existingDiagram();

    importFrom(diagram, fileWith(directory, "@startuml\nclass A\nA --> \n@enduml\n"));

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertEquals(List.of("msg.plantuml.file.invalid"), specman.shownMessages());
  }

  /** Scenario: The user cancels the dialog. Nothing changes. */
  @Test
  void testCancellingChangesNothing() {
    PlantUmlImageEditArea diagram = existingDiagram();

    importFrom(diagram, PlantUmlFileChoice.cancelled());

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: In change mode, an import is a source change like any other: The source from before is kept as
   * backup, and rejecting the changes brings it back. */
  @Test
  void testImportInChangeModeKeepsTheFormerSourceAsBackup(@TempDir Path directory) throws IOException {
    PlantUmlImageEditArea diagram = existingDiagram();
    ChangeSet blue = ChangeSet.ALL.get("blue");
    specman.toggleChangeMode();
    specman.selectChangeSet(blue);

    importFrom(diagram, fileWith(directory, THREE_CLASSES));

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertNotNull(diagram.getBackup(), "backup");
    assertEquals(TWO_CLASSES, diagram.getBackup().source());
    assertEquals(blue.panelColor(), diagram.getBackground());

    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertNull(diagram.getBackup());
  }

  private static PlantUmlImageEditArea existingDiagram() {
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());
    diagram.commitSource(TWO_CLASSES);
    return diagram;
  }

  private static PlantUmlFileChoice fileWith(Path directory, String content) throws IOException {
    Path file = directory.resolve("diagram.puml");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return PlantUmlFileChoice.file(file.toFile());
  }

  private static void importFrom(PlantUmlImageEditArea diagram, PlantUmlFileChoice choice) {
    specman.answerPlantUmlFileChoiceWith(choice);
    specman.clickContextMenuItem(diagram, PlantUmlContextMenu.IMPORT_ITEM);
  }
}
