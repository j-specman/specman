package headless.image.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.image.plantuml.PlantUmlRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Relative paths in {@code !include} are resolved against the directory of the model file. */
class PlantUmlIncludeTest extends AbstractPlantUmlTest {
  private static final String INCLUDING_SOURCE = "@startuml\n!include shared/common.puml\nclass GREETING\n@enduml\n";
  private static final String INCLUDED_NAME = "HelloFromInclude";

  @Test
  void testIncludeIsResolvedRelativeToTheModelFile(@TempDir Path projectDirectory) throws IOException {
    writeSharedFile(projectDirectory);
    specman.setDiagrammDatei(projectDirectory.resolve("model.nsd").toFile());

    assertTrue(svgOf(INCLUDING_SOURCE).contains(INCLUDED_NAME), "Included definition must be applied");
  }

  @Test
  void testIncludeFollowsTheModelFileWhenItMovesToAnotherDirectory(@TempDir Path directory) throws IOException {
    Path projectDirectory = Files.createDirectory(directory.resolve("project"));
    Path otherDirectory = Files.createDirectory(directory.resolve("other"));
    writeSharedFile(projectDirectory);
    specman.setDiagrammDatei(projectDirectory.resolve("model.nsd").toFile());
    assertTrue(svgOf(INCLUDING_SOURCE).contains(INCLUDED_NAME), "Precondition: include works next to the model file");

    specman.setDiagrammDatei(otherDirectory.resolve("model.nsd").toFile());

    assertFalse(svgOf(INCLUDING_SOURCE).contains(INCLUDED_NAME), "The include must not be found any more");
  }

  private static void writeSharedFile(Path projectDirectory) throws IOException {
    File shared = projectDirectory.resolve("shared/common.puml").toFile();
    Files.createDirectories(shared.getParentFile().toPath());
    Files.writeString(shared.toPath(), "!define GREETING " + INCLUDED_NAME + "\n", StandardCharsets.UTF_8);
  }

  private static String svgOf(String source) {
    return new String(PlantUmlRenderer.renderSvg(source), StandardCharsets.UTF_8);
  }
}
