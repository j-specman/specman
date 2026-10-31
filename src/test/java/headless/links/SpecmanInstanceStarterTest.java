package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.SpecmanInstanceStarter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Works on a fake Java runtime in a temporary directory, no process is started. */
class SpecmanInstanceStarterTest {

  private static final String LINK = "specman://uc720,2d34f85e,1.3";
  private static final String CLASS_PATH = "app/specman.jar;app/lib.jar";

  @TempDir
  Path javaHome;

  private void createExecutables(String... names) throws IOException {
    Files.createDirectory(javaHome.resolve("bin"));
    for (String name : names) {
      Files.createFile(javaHome.resolve("bin").resolve(name));
    }
  }

  private List<String> commandLine() throws IOException {
    return new SpecmanInstanceStarter(javaHome.toFile(), CLASS_PATH).commandLine(LINK);
  }

  private String executable(String name) {
    return javaHome.resolve("bin").resolve(name).toString();
  }

  /** The new process runs the Specman main class with the running instance's class path and gets the link as its only argument. */
  @Test
  void testCommandLineStartsSpecmanWithLink() throws IOException {
    createExecutables("javaw.exe");

    assertEquals(List.of(executable("javaw.exe"), "-Dfile.encoding=UTF-8", "-cp", CLASS_PATH, "specman.Specman", LINK), commandLine());
  }

  /** The Java executable which opens no console window is preferred, with the others as fallbacks. */
  @Test
  void testPrefersJavawOverJavaExe() throws IOException {
    createExecutables("java.exe", "javaw.exe", "java");

    assertEquals(executable("javaw.exe"), commandLine().get(0));
  }

  @Test
  void testFallsBackToJavaExe() throws IOException {
    createExecutables("java.exe", "java");

    assertEquals(executable("java.exe"), commandLine().get(0));
  }

  /** Scenario: Other platforms have no ".exe". */
  @Test
  void testFallsBackToJavaWithoutExtension() throws IOException {
    createExecutables("java");

    assertEquals(executable("java"), commandLine().get(0));
  }

  @Test
  void testFailsWithoutJavaExecutable() throws IOException {
    createExecutables();

    assertThrows(IOException.class, this::commandLine);
  }

  @Test
  void testFailsWithoutRuntime() {
    SpecmanInstanceStarter starter = new SpecmanInstanceStarter(new File("Z:/no/such/runtime"), CLASS_PATH);

    assertThrows(IOException.class, () -> starter.commandLine(LINK));
  }
}
