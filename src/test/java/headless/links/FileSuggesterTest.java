package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import specman.link.LinkAliases;
import specman.link.suggestion.FileSuggester;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Works on real files in a temporary directory, which are searched for the one an alias stands for.
 * Needs neither a Specman instance nor the user preferences. */
class FileSuggesterTest {

  private static final String EXAMPLE_NAME = "BALI UC-730 Kundendaten pflegen.nsd";

  @TempDir
  Path directory;

  private Path createOtherDirectory() throws IOException {
    return Files.createDirectory(directory.resolve("other"));
  }

  private File create(String fileName) throws IOException {
    return Files.createFile(directory.resolve(fileName)).toFile();
  }

  private LinkAliases.Entry example(String fileName, String alias) {
    return new LinkAliases.Entry(directory.resolve(fileName).toFile(), alias);
  }

  private static File suggest(List<LinkAliases.Entry> examples, String alias) {
    return new FileSuggester(examples).suggest(alias);
  }

  private File suggest(String exampleName, String exampleAlias, String alias) {
    return suggest(List.of(example(exampleName, exampleAlias)), alias);
  }

  /** The file is the one which has the alias' characters at the positions the example's alias found its characters. */
  @ParameterizedTest(name = "{1} for {0} finds {3} for {2}")
  @CsvSource(delimiter = '|', value = {
    // The example from the original concept
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | uc135 | BALI UC-135 Was Anderes.nsd",
    // The alias' letter case doesn't matter, nor does the file name's
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | UC135 | BALI uc-135 Was Anderes.nsd",
    // Hyphen and underscore are one kind of separator, in the alias as well as in the file names
    "BALI UC_730 Kundendaten pflegen.nsd | uc-730 | uc-135 | BALI UC_135 Was Anderes.nsd",
    "BALI UC-730 Kundendaten pflegen.nsd | uc_730 | uc-135 | BALI UC-135 Was Anderes.nsd",
    "BALI UC-730 Kundendaten pflegen.nsd | uc-730 | uc_135 | BALI UC-135 Was Anderes.nsd",
    // Only the positions of the alias' characters count
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | uc135 | BALI UC-135.nsd",
    "BALI UC-730 Kundendaten pflegen.nsd | bali730 | abcd135 | ABCD UC-135 Was Anderes.nsd",
  })
  void testFindsFileWithTheAliasCharactersAtThePositionsOfTheExample(
    String exampleName, String exampleAlias, String alias, String expectedName) throws IOException {
    create(exampleName.trim());
    File expected = create(expectedName.trim());

    assertEquals(expected, suggest(exampleName.trim(), exampleAlias.trim(), alias.trim()));
  }

  /** The new alias must be made like the example's alias, otherwise the example doesn't tell anything about it. */
  @ParameterizedTest(name = "{0} is no pattern for {1}")
  @CsvSource(delimiter = '|', value = {
    // Longer or shorter
    "uc730 | uc1350",
    "uc730 | uc13",
    // Letters where the example has digits
    "uc730 | ucabc",
    // Digits where the example has letters
    "uc730 | 1c135",
    // No separator where the example has one
    "uc-730 | uc1350",
    // Nothing to compare
    "uc730 | ''",
  })
  void testAliasOfOtherStructureFindsNothing(String exampleAlias, String alias) throws IOException {
    create(EXAMPLE_NAME);
    create("BALI UC-135 Was Anderes.nsd");
    create("BALI UC-1350 Was Anderes.nsd");

    assertNull(suggest(EXAMPLE_NAME, exampleAlias, alias));
  }

  /** The file of the pattern must exist, with the right characters in the right places. */
  @Test
  void testFindsNothingWithoutFileFollowingThePattern() throws IOException {
    create(EXAMPLE_NAME);
    create("BALI UC-136 Was Anderes.nsd");
    create("testusecase.nsd");

    assertNull(suggest(EXAMPLE_NAME, "uc730", "uc135"));
  }

  /** Only diagrams count, other files with the matching name are of no interest. */
  @Test
  void testIgnoresFilesWhichAreNoDiagrams() throws IOException {
    create(EXAMPLE_NAME);
    create("BALI UC-135 Was Anderes.txt");
    create("BALI UC-135 Was Anderes.nsd.wrk");
    File diagram = create("BALI UC-135 Was Anderes.NSD");

    assertEquals(diagram, suggest(EXAMPLE_NAME, "uc730", "uc135"));
  }

  /** Scenario: Two files follow the pattern. Guessing would send the user to the wrong one in half of the cases,
   * so the user chooses without a suggestion. */
  @Test
  void testFindsNothingIfSeveralFilesFollowThePattern() throws IOException {
    create(EXAMPLE_NAME);
    create("BALI UC-135 Was Anderes.nsd");
    create("BALI UC-135 Noch was.nsd");

    assertNull(suggest(EXAMPLE_NAME, "uc730", "uc135"));
  }

  /** Scenario: The example is in another directory than the file the alias stands for. That is no basis for a suggestion,
   * the directory of the example is the one searched. */
  @Test
  void testSearchesTheDirectoryOfTheExampleOnly() throws IOException {
    Files.createFile(createOtherDirectory().resolve("BALI UC-135 Was Anderes.nsd"));
    create(EXAMPLE_NAME);

    assertNull(suggest(EXAMPLE_NAME, "uc730", "uc135"));
  }

  /** Scenario: The directory of the example is gone, e.g. a network drive which isn't connected. */
  @Test
  void testFindsNothingIfDirectoryOfExampleDoesntExist() {
    LinkAliases.Entry example = new LinkAliases.Entry(new File("Z:/no/such/dir/" + EXAMPLE_NAME), "uc730");

    assertNull(suggest(List.of(example), "uc135"));
  }

  /** Scenario: Several examples in the directory are made the same way and all find the same file. That's one file,
   * so it is suggested. */
  @Test
  void testSameFileFoundBySeveralExamplesIsSuggested() throws IOException {
    create(EXAMPLE_NAME);
    create("BALI UC-731 Anderes.nsd");
    File file = create("BALI UC-135 Was Anderes.nsd");
    List<LinkAliases.Entry> examples = List.of(example(EXAMPLE_NAME, "uc730"), example("BALI UC-731 Anderes.nsd", "uc731"));

    assertEquals(file, suggest(examples, "uc135"));
  }

  /** Scenario: Two projects in different directories are named the same way, so each of them has a file for the alias.
   * The user has to choose which one is meant. */
  @Test
  void testFindsNothingIfExamplesInDifferentDirectoriesFindDifferentFiles() throws IOException {
    Path otherDirectory = createOtherDirectory();
    create(EXAMPLE_NAME);
    create("BALI UC-135 Was Anderes.nsd");
    Files.createFile(otherDirectory.resolve("BALI UC-731 Anderes.nsd"));
    Files.createFile(otherDirectory.resolve("BALI UC-135 Was Anderes.nsd"));
    List<LinkAliases.Entry> examples = List.of(
      example(EXAMPLE_NAME, "uc730"),
      new LinkAliases.Entry(otherDirectory.resolve("BALI UC-731 Anderes.nsd").toFile(), "uc731"));

    assertNull(suggest(examples, "uc135"));
  }


  /** Scenario: Secondary use cases like "UC-200-02" have their own alias, which is the pattern for aliases made like it.
   * The alias of a primary use case doesn't fit its structure. */
  @Test
  void testAliasOfSecondaryUseCaseFindsSecondaryUseCaseFile() throws IOException {
    create("BALI UC-200 Primary.nsd");
    create("BALI UC-200-02 Secondary.nsd");
    File secondary = create("BALI UC-315-01 Other secondary.nsd");
    List<LinkAliases.Entry> examples = List.of(
      example("BALI UC-200 Primary.nsd", "uc200"),
      example("BALI UC-200-02 Secondary.nsd", "uc200-02"));

    assertEquals(secondary, suggest(examples, "uc315-01"));
  }

  /** A pattern without a file doesn't prevent a shorter pattern from finding one. */
  @Test
  void testPatternWithoutFileIsSkipped() throws IOException {
    create(EXAMPLE_NAME);
    create("Protokoll 2026-10-29.nsd");
    File file = create("BALI UC-135 Was Anderes.nsd");
    List<LinkAliases.Entry> examples = List.of(
      example("Protokoll 2026-10-29.nsd", "protokoll2026"),
      example(EXAMPLE_NAME, "uc730"));

    assertEquals(file, suggest(examples, "uc135"));
  }

  /** Without examples there is nothing to suggest. */
  @Test
  void testFindsNothingWithoutExamples() throws IOException {
    create("BALI UC-135 Was Anderes.nsd");

    assertNull(suggest(List.of(), "uc135"));
  }
}
