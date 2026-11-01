package headless.image.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;
import specman.view.AbstractSchrittView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The PlantUML source is persisted as readable text, so that agents can read and edit it in the file. */
class PlantUmlPersistenceTest extends AbstractPlantUmlTest {

  @Test
  void testSourceSurvivesSavingAndLoadingTheWholeDiagram(@TempDir Path directory) {
    insertDiagramByToolbarButton(containerOfNewStep()).commitSource(TWO_CLASSES);

    saveAndLoad(directory);

    assertEquals(TWO_CLASSES, reloadedDiagram().getSource());
  }

  @Test
  void testSavedFileHoldsTheSourceAsTextAndNoImageData(@TempDir Path directory) throws IOException {
    insertDiagramByToolbarButton(containerOfNewStep()).commitSource(TWO_CLASSES);

    File file = save(directory);

    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    assertTrue(saved.contains("type=plantuml"), "image type");
    assertTrue(saved.contains("`" + TWO_CLASSES + "`"), "source in backticks");
    assertFalse(saved.contains("%, type=png"), "no bitmap must be stored for a PlantUML diagram");
  }

  @Test
  void testSourceEditedInTheFileIsShownAfterLoading(@TempDir Path directory) throws IOException {
    insertDiagramByToolbarButton(containerOfNewStep()).commitSource(TWO_CLASSES);
    File file = save(directory);
    String editedByAgent = Files.readString(file.toPath(), StandardCharsets.UTF_8)
      .replace(TWO_CLASSES, THREE_CLASSES);
    Files.writeString(file.toPath(), editedByAgent, StandardCharsets.UTF_8);

    specman.diagrammLaden(file);

    assertEquals(THREE_CLASSES, reloadedDiagram().getSource());
  }

  @Test
  void testReloadedDiagramIsRenderedAndHasTheUndoneSourceAfterwards(@TempDir Path directory) {
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());
    diagram.commitSource(TWO_CLASSES);
    saveAndLoad(directory);
    PlantUmlImageEditArea reloaded = reloadedDiagram();
    specman.layoutComponents();

    reloaded.changeSource(THREE_CLASSES);
    specman.undo();

    assertTrue(reloaded.getFullSizeImage().getWidth() > 0, "The reloaded diagram must be rendered");
    assertEquals(TWO_CLASSES, reloaded.getSource());
  }

  private static File save(Path directory) {
    File file = directory.resolve("plantuml.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    return file;
  }

  private static void saveAndLoad(Path directory) {
    specman.diagrammLaden(save(directory));
  }

  private static PlantUmlImageEditArea reloadedDiagram() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    EditContainer container = step.getFirstEditArea().asTextArea().getParent();
    return (PlantUmlImageEditArea) container.getEditAreaAt(1);
  }
}
