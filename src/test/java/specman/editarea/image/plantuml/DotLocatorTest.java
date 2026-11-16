package specman.editarea.image.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DotLocatorTest {
  private static final String EXECUTABLE = "dot";
  private static final Predicate<File> ANY_FILE_WORKS = file -> true;

  @TempDir
  Path installations;

  @Test
  void environmentVariableWinsOverPath() throws IOException {
    File fromVariable = installDotIn("variable");
    File fromPath = installDotIn("path");
    Map<String, String> environment = Map.of(
      DotLocator.ENVIRONMENT_VARIABLE, fromVariable.getPath(),
      "PATH", fromPath.getParent());

    DotLocator locator = new DotLocator(environment, EXECUTABLE, ANY_FILE_WORKS);

    assertEquals(fromVariable, locator.locate());
  }

  @Test
  void pathIsSearchedInItsOrderAndSkipsDirectoriesWithoutDot() throws IOException {
    File emptyDirectory = Files.createDirectory(installations.resolve("empty")).toFile();
    File first = installDotIn("first");
    File second = installDotIn("second");
    String path = String.join(File.pathSeparator, emptyDirectory.getPath(), first.getParent(), second.getParent());

    DotLocator locator = new DotLocator(Map.of("PATH", path), EXECUTABLE, ANY_FILE_WORKS);

    assertEquals(first, locator.locate());
  }

  @Test
  void environmentVariablePointingToNothingFallsThroughToPath() throws IOException {
    File fromPath = installDotIn("path");
    Map<String, String> environment = Map.of(
      DotLocator.ENVIRONMENT_VARIABLE, installations.resolve("gone").resolve(EXECUTABLE).toString(),
      "PATH", fromPath.getParent());

    DotLocator locator = new DotLocator(environment, EXECUTABLE, ANY_FILE_WORKS);

    assertEquals(fromPath, locator.locate());
  }

  @Test
  void aDotWhichDoesNotWorkIsSkipped() throws IOException {
    File broken = installDotIn("broken");
    File working = installDotIn("working");
    String path = String.join(File.pathSeparator, broken.getParent(), working.getParent());

    DotLocator locator = new DotLocator(Map.of("PATH", path), EXECUTABLE, file -> !file.equals(broken));

    assertEquals(working, locator.locate());
  }

  @Test
  void namesOfEnvironmentVariablesAreMatchedIgnoringCase() throws IOException {
    File fromPath = installDotIn("path");
    File fromVariable = installDotIn("variable");

    assertEquals(fromPath, new DotLocator(Map.of("Path", fromPath.getParent()), EXECUTABLE, ANY_FILE_WORKS).locate());
    assertEquals(fromVariable, new DotLocator(Map.of("Graphviz_Dot", fromVariable.getPath()), EXECUTABLE, ANY_FILE_WORKS).locate());
  }

  @Test
  void nothingIsFoundWithoutDotInVariableOrPath() {
    DotLocator locator = new DotLocator(Map.of("PATH", installations.toString()), EXECUTABLE, ANY_FILE_WORKS);

    assertNull(locator.locate());
  }

  private File installDotIn(String directoryName) throws IOException {
    Path directory = Files.createDirectory(installations.resolve(directoryName));
    return Files.createFile(directory.resolve(EXECUTABLE)).toFile();
  }
}
