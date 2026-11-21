package classman;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classman is meant to leave Specman one day, so nothing in classman.* may refer to specman.*. */
class ClassmanIndependenceTest {
  private static final Path MAIN_SOURCES = Paths.get("src/main/java/classman");
  private static final Path TEST_SOURCES = Paths.get("src/test/java/classman");
  private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
  private static final Pattern SPECMAN_PACKAGE = Pattern.compile("(?<![\\w.])specman\\.\\w");

  @Test
  void mainCodeIsIndependentOfSpecman() throws IOException {
    assertNoSpecmanReferencesIn(MAIN_SOURCES);
  }

  @Test
  void testCodeIsIndependentOfSpecman() throws IOException {
    assertNoSpecmanReferencesIn(TEST_SOURCES);
  }

  private static void assertNoSpecmanReferencesIn(Path root) throws IOException {
    List<Path> sources = javaFilesIn(root);
    assertTrue(sources.size() > 10, "no Java files found below " + root.toAbsolutePath());
    List<String> offenders = sources.stream()
      .filter(ClassmanIndependenceTest::refersToSpecman)
      .map(Path::toString)
      .collect(Collectors.toList());
    assertEquals(List.of(), offenders);
  }

  private static List<Path> javaFilesIn(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      return files.filter(file -> file.toString().endsWith(".java")).collect(Collectors.toList());
    }
  }

  private static boolean refersToSpecman(Path source) {
    try {
      String code = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
      return SPECMAN_PACKAGE.matcher(COMMENTS.matcher(code).replaceAll("")).find();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
