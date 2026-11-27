package specman.link.classman;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.LinkAliases;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Following a Classman link to the file with the diagram, and what can be wrong on the way. */
class ClassmanLinkResolverTest {
  private static final String DIAGRAM = "classdiagram \"Hilfe\" (0a1b2c3d) {\n    class A (aaaaaaaa)\n}\n";

  private final ClassmanLinkResolver resolver = new ClassmanLinkResolver();
  private Preferences testPreferences;

  @BeforeEach
  void useOwnAliases() {
    testPreferences = Preferences.userRoot().node("specman-test-" + UUID.randomUUID());
    LinkAliases.useNode(testPreferences);
  }

  @AfterEach
  void restoreAliases() throws BackingStoreException {
    LinkAliases.useNode(null);
    testPreferences.removeNode();
  }

  private static File write(Path directory, String name, String content) throws IOException {
    Path file = directory.resolve(name);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  /** Scenario: The link names the diagram by a path relative to the directory of the document which contains the link. */
  @Test
  void relativePathStartsInTheDirectoryOfTheDocument(@TempDir Path directory) throws IOException {
    File diagram = write(directory, "diagrams/hilfe.cld", DIAGRAM);
    File document = directory.resolve("specs/UC-720.nsd").toFile();

    ClassmanLinkResolution resolution = new ClassmanLinkResolver(document).resolve("classman://../diagrams/hilfe.cld");

    assertTrue(resolution.isOk());
    assertEquals(diagram, resolution.file());
  }

  @Test
  void linkByPathLeadsToTheDiagram(@TempDir Path directory) throws IOException {
    File file = write(directory, "hilfe.cld", DIAGRAM);

    ClassmanLinkResolution resolution = resolver.resolve(ClassmanLink.of(file, "", null, null));

    assertTrue(resolution.isOk());
    assertEquals(file, resolution.file());
    assertEquals(DIAGRAM, resolution.source());
    assertEquals("0a1b2c3d", resolution.diagramId());
    assertEquals("Hilfe", resolution.diagramName());
  }

  @Test
  void linkByAliasLeadsToTheDiagram(@TempDir Path directory) throws IOException {
    File file = write(directory, "hilfe.cld", DIAGRAM);
    LinkAliases.assign(file, "hilfe");

    ClassmanLinkResolution resolution = resolver.resolve("classman://hilfe");

    assertTrue(resolution.isOk());
    assertEquals(DIAGRAM, resolution.source());
  }

  @Test
  void textWithoutSchemeIsNoLink() {
    ClassmanLinkResolution resolution = resolver.resolve("hilfe");

    assertEquals(ClassmanLinkProblem.NOT_A_LINK, resolution.problem());
    assertEquals("hilfe", resolution.problemDetail());
    assertNull(resolution.file());
  }

  @Test
  void unknownAliasLeadsNowhere() {
    ClassmanLinkResolution resolution = resolver.resolve("classman://nobody");

    assertEquals(ClassmanLinkProblem.ALIAS_UNKNOWN, resolution.problem());
    assertEquals("nobody", resolution.problemDetail());
    assertNull(resolution.file());
  }

  @Test
  void aliasOfMissingFileLeadsToTheMissingFile(@TempDir Path directory) {
    File gone = directory.resolve("gone.cld").toFile();
    LinkAliases.assign(gone, "gone");

    ClassmanLinkResolution resolution = resolver.resolve("classman://gone");

    assertEquals(ClassmanLinkProblem.FILE_MISSING, resolution.problem());
    assertEquals(gone, resolution.file());
    assertNull(resolution.source());
  }

  @Test
  void onlyDiagramFilesAreRead(@TempDir Path directory) throws IOException {
    File file = write(directory, "hilfe.txt", DIAGRAM);

    ClassmanLinkResolution resolution = resolver.resolve(ClassmanLink.of(file, "", null, null));

    assertEquals(ClassmanLinkProblem.WRONG_EXTENSION, resolution.problem());
    assertNull(resolution.source());
  }

  @Test
  void extensionIsNotCaseSensitive(@TempDir Path directory) throws IOException {
    File file = write(directory, "HILFE.CLD", DIAGRAM);

    assertTrue(resolver.resolve(file).isOk());
  }

  @Test
  void fileWhichIsNoDiagramIsInvalidButRead(@TempDir Path directory) throws IOException {
    File file = write(directory, "broken.cld", "this is no diagram");

    ClassmanLinkResolution resolution = resolver.resolve(file);

    assertFalse(resolution.isOk());
    assertEquals(ClassmanLinkProblem.NOT_A_DIAGRAM, resolution.problem());
    assertEquals("this is no diagram", resolution.source());
    assertNull(resolution.diagramId());
  }

  @Test
  void lineBreaksAndByteOrderMarkAreNormalized(@TempDir Path directory) throws IOException {
    File file = write(directory, "hilfe.cld", (char) 0xFEFF + DIAGRAM.replace("\n", "\r\n"));

    assertEquals(DIAGRAM, resolver.resolve(file).source());
  }
}
