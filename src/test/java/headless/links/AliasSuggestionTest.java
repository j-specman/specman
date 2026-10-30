package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import specman.link.LinkAliases;
import specman.link.suggestion.AliasSuggestion;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Works on plain file names and aliases, needs neither a Specman instance nor the user preferences. */
class AliasSuggestionTest {

  private static final String PROJECT = "C:/project/";
  private static final String EXAMPLE_NAME = "BALI UC-730 Kundendaten pflegen.nsd";
  private static final String NEW_NAME = "BALI UC-135 Was Anderes.nsd";

  private static LinkAliases.Entry entry(String fileName, String alias) {
    return new LinkAliases.Entry(new File(PROJECT + fileName), alias);
  }

  private static String suggest(List<LinkAliases.Entry> entries, String newFileName) {
    return new AliasSuggestion(entries).suggest(new File(PROJECT + newFileName));
  }

  private static String suggest(String exampleName, String exampleAlias, String newName) {
    return suggest(List.of(entry(exampleName, exampleAlias)), newName);
  }

  /** The suggestion is made of the characters at the positions where the example's alias found its characters, taking over
   * letter case and kind of separator from the alias. */
  @ParameterizedTest(name = "{1} for {0} makes {3} for {2}")
  @CsvSource(delimiter = '|', value = {
    // The example of the todo
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI UC-135 Was Anderes.nsd | uc135",
    // Characters after the ones used by the alias may be anything, or missing
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI UC-135.nsd | uc135",
    // Only the positions of the alias' characters count, not what's in front of them
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | 2026 UC-135 Was Anderes.nsd | uc135",
    // The new file name's letter case doesn't matter, the alias' does
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI Uc-135 Was Anderes.nsd | uc135",
    "BALI UC-730 Kundendaten pflegen.nsd | UC730 | BALI uc-135 Was Anderes.nsd | UC135",
    "BALI UC-730 Kundendaten pflegen.nsd | Uc730 | BALI uc-135 Was Anderes.nsd | Uc135",
    // Hyphen and underscore are one kind of separator in the file names. The alias decides which one is used
    "BALI UC_730 Kundendaten pflegen.nsd | uc-730 | BALI UC_135 Was Anderes.nsd | uc-135",
    "BALI UC-730 Kundendaten pflegen.nsd | uc_730 | BALI UC-135 Was Anderes.nsd | uc_135",
    "BALI UC-730 Kundendaten pflegen.nsd | uc_730 | BALI UC_135 Was Anderes.nsd | uc_135",
    // Several separators
    "BALI UC-730-02 Kunde.nsd | uc730-02 | BALI UC-135-07 Was.nsd | uc135-07",
    // Characters may have gaps, the new file's letters are taken over at the same positions
    "BALI UC-730 Kundendaten pflegen.nsd | bali730 | ABCD UC-135 Was Anderes.nsd | abcd135",
    // The extension is no part of the name
    "UC-730.nsd | uc730 | UC-135.xyz | uc135",
  })
  void testSuggestsCharactersAtThePositionsOfTheExample(String exampleName, String exampleAlias, String newName, String expected) {
    assertEquals(expected, suggest(exampleName.trim(), exampleAlias.trim(), newName.trim()));
  }

  /** The new file name must have the same kind of character (letter, digit, separator) as the example at every position used. */
  @ParameterizedTest(name = "{1} for {0} makes no suggestion for {2}")
  @CsvSource(delimiter = '|', value = {
    // Letters where the example has digits
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI UC-ABC Was Anderes.nsd",
    // Digits where the example has letters
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI 2026-10-29.nsd",
    // No separator where the example has one
    "BALI UC-730-02 Kunde.nsd | uc730-02 | BALI UC-135 Was.nsd",
    // Too short
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | BALI.nsd",
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | UC-135.nsd",
    // Same length, but letters in the wrong places
    "BALI UC-730 Kundendaten pflegen.nsd | uc730 | testusecase.nsd",
    // Letters are ASCII letters only, as the aliases are
    "UC-730.nsd | uc730 | \u00dcC-135.nsd",
  })
  void testSuggestsNothingForFileNameOfOtherStructure(String exampleName, String exampleAlias, String newName) {
    assertNull(suggest(exampleName.trim(), exampleAlias.trim(), newName.trim()));
  }

  /** Nothing can be learned from an alias whose characters can't be found in order in the name of its file. */
  @ParameterizedTest(name = "{0} is no example for {1}")
  @CsvSource(delimiter = '|', value = {
    // Nothing in common
    "xyz | BALI UC-730 Kundendaten pflegen.nsd",
    // A character which isn't in the file name
    "uc730x | BALI UC-730 Kundendaten pflegen.nsd",
    // Characters in the wrong order
    "730uc | BALI UC-730 Kundendaten pflegen.nsd",
    // Nothing to derive from
    "'' | BALI UC-730 Kundendaten pflegen.nsd",
  })
  void testLearnsNothingFromAliasNotFoundInItsFileName(String exampleAlias, String exampleName) {
    assertNull(suggest(exampleName.trim(), exampleAlias, exampleName.trim()));
  }

  /** Examples are only taken from the file's own directory, even if a file in another directory is named the same way. */
  @Test
  void testSuggestsNothingFromExampleInOtherDirectory() {
    List<LinkAliases.Entry> entries = List.of(entry(EXAMPLE_NAME, "uc730"));

    String suggestion = new AliasSuggestion(entries).suggest(new File("D:/other/dir/" + NEW_NAME));

    assertNull(suggestion);
  }

  /** Scenario: A very short alias like "egal" for the file "egal.nsd" in another directory would match any file name which
   * starts with letters. It must not get in the way of a fitting example in the file's own directory. */
  @Test
  void testExampleInOtherDirectoryDoesntHideFittingExampleInOwnDirectory() {
    List<LinkAliases.Entry> entries = List.of(
      new LinkAliases.Entry(new File("C:/Users/jl/Documents/test/egal.nsd"), "egal"),
      entry(EXAMPLE_NAME, "uc730"));

    assertEquals("uc135", suggest(entries, NEW_NAME));
  }

  /** The suggestion goes back to the longest alias, regardless of the order of the examples. */
  @Test
  void testLongestAliasWins() {
    assertEquals("uc135", suggest(List.of(entry(EXAMPLE_NAME, "bali"), entry(EXAMPLE_NAME, "uc730")), NEW_NAME));
    assertEquals("uc135", suggest(List.of(entry(EXAMPLE_NAME, "uc730"), entry(EXAMPLE_NAME, "bali")), NEW_NAME));
  }

  /** Scenario: Secondary use cases like "UC-200-02" have their own alias, which is preferred for files named the same way.
   * Files of primary use cases don't fit its pattern, so the alias of a primary use case is used for them. */
  @Test
  void testAliasOfSecondaryUseCaseIsPreferredForSecondaryUseCases() {
    List<LinkAliases.Entry> entries = List.of(
      entry("BALI UC-200 Primary.nsd", "uc200"),
      entry("BALI UC-200-02 Secondary.nsd", "uc200-02"));

    assertEquals("uc315-01", suggest(entries, "BALI UC-315-01 Other secondary.nsd"));
    assertEquals("uc315", suggest(entries, "BALI UC-315 Other primary.nsd"));
  }

  /** Scenario: Numbers of different length make the first suggestion wrong. Once the user has corrected it, the corrected
   * alias is the longest and used for the new numbers of that length, while the shorter numbers still follow the old example. */
  @Test
  void testWrongSuggestionForLongerNumberHealsWithCorrectedAlias() {
    assertEquals("uc135", suggest(EXAMPLE_NAME, "uc730", "BALI UC-1350 Was Anderes.nsd"));

    List<LinkAliases.Entry> entries = List.of(
      entry(EXAMPLE_NAME, "uc730"),
      entry("BALI UC-1350 Was Anderes.nsd", "uc1350"));

    assertEquals("uc1400", suggest(entries, "BALI UC-1400 Noch was.nsd"));
    assertEquals("uc135", suggest(entries, "BALI UC-135 Noch was.nsd"));
  }

  /** The examples with the same length of alias are ranked in the given order, the first one wins. */
  @Test
  void testFirstOfEquallyLongAliasesWins() {
    String newName = "BALI UC-135 Was Anderes.nsd";

    assertEquals("uc135", suggest(List.of(entry(EXAMPLE_NAME, "uc730"), entry(EXAMPLE_NAME, "ba730")), newName));
    assertEquals("ba135", suggest(List.of(entry(EXAMPLE_NAME, "ba730"), entry(EXAMPLE_NAME, "uc730")), newName));
  }

  /** An example the file doesn't match is skipped, even if its alias is longer. */
  @Test
  void testSkipsExampleTheFileDoesntMatch() {
    List<LinkAliases.Entry> entries = List.of(
      entry("Protokoll 2026-10-29.nsd", "protokoll2026"),
      entry(EXAMPLE_NAME, "uc730"));

    assertEquals("uc135", suggest(entries, NEW_NAME));
  }

  /** Without examples there is nothing to suggest. */
  @Test
  void testSuggestsNothingWithoutExamples() {
    assertNull(suggest(List.of(), NEW_NAME));
  }
}
