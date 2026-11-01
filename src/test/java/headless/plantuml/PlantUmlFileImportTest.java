package headless.plantuml;

import net.atlanticbb.tantlinger.ui.text.actions.PlantUmlAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A new PlantUML diagram can take its source from a file. Only a file with source which the editor would accept as well
 * is taken over. */
class PlantUmlFileImportTest extends AbstractPlantUmlTest {

  private static final String REFUSED_MESSAGE_KEY = "msg.plantuml.file.invalid";

  /** Scenario: The user chooses a file with valid source. The diagram shows its content, and a single undo takes the
   * whole diagram out again. */
  @Test
  void testDiagramTakesTheSourceOfTheChosenFile(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, TWO_CLASSES));

    assertTrue(hasDiagram(container), "diagram must be inserted");
    assertEquals(TWO_CLASSES, diagramIn(container).getSource());
    assertEquals(List.of(), specman.shownMessages());
    assertTrue(specman.plantUmlEmptyDiagramWasOffered(), "starting empty must be offered for a new diagram");

    specman.undo();

    assertFalse(hasDiagram(container), "a single undo must take the diagram out");
  }

  /** Scenario: Files from Windows editors have CRLF line breaks and possibly a byte order mark. The source gets the
   * line breaks Specman uses and the mark is dropped. */
  @Test
  void testWindowsLineBreaksAndByteOrderMarkAreNormalized(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();
    String windowsContent = (char) 0xFEFF + TWO_CLASSES.replace("\n", "\r\n");

    insertFrom(fileWith(directory, windowsContent));

    assertEquals(TWO_CLASSES, diagramIn(container).getSource());
  }

  /** Scenario: The file has a syntax error. No diagram is created, and the user gets told why. */
  @Test
  void testFileWithSyntaxErrorIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, "@startuml\nclass A\nA --> \n@enduml\n"));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(REFUSED_MESSAGE_KEY), specman.shownMessages());
  }

  /** Scenario: The file breaks one of the rules for the source in an editor, e.g. a second diagram. It is refused,
   * too. */
  @Test
  void testFileBreakingTheEditorRulesIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    insertFrom(fileWith(directory, TWO_CLASSES + TWO_CLASSES));

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(REFUSED_MESSAGE_KEY), specman.shownMessages());
  }

  /** Scenario: The user cancels the dialog. Nothing is inserted. */
  @Test
  void testCancellingInsertsNothing() {
    EditContainer container = containerOfNewStep();

    insertFrom(PlantUmlFileChoice.cancelled());

    assertFalse(hasDiagram(container), "no diagram must be inserted");
    assertEquals(List.of(), specman.shownMessages());
  }

  private static PlantUmlFileChoice fileWith(Path directory, String content) throws IOException {
    Path file = directory.resolve("diagram.puml");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return PlantUmlFileChoice.file(file.toFile());
  }

  private static void insertFrom(PlantUmlFileChoice choice) {
    specman.answerPlantUmlFileChoiceWith(choice);
    specman.doEdit(PlantUmlAction.class);
    specman.layoutComponents();
  }

  private static PlantUmlImageEditArea diagramIn(EditContainer container) {
    return (PlantUmlImageEditArea) container.getEditAreaAt(1);
  }
}
