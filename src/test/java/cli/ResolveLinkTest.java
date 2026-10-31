package cli;

import builder.DiagrammBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.LinkAliases;
import specman.link.SpecmanLink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static builder.BuilderFactory.whileStep;
import static cli.SpecmanCLIFT.resolveLinks;
import static cli.SpecmanCLIFT.resolveLinksFromStdin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the {@code --resolve-link} command line operation: For each link exactly one line of
 * status, file and detail, separated by tabs. */
public class ResolveLinkTest {

  private static final String TAB = "\t";
  private static final String STEP_ID = "1e76071a";

  private Preferences testPreferences;

  @BeforeAll
  static void deleteOldNsdFiles() throws IOException {
    DiagrammBuilder.deleteOldNsdFiles();
  }

  @BeforeEach
  void useOwnAliases() {
    testPreferences = Preferences.userRoot().node("specman-test-" + UUID.randomUUID());
    LinkAliases.useNode(testPreferences);
  }

  @AfterEach
  void dropOwnAliases() throws BackingStoreException {
    LinkAliases.useNode(null);
    testPreferences.removeNode();
  }

  /** A diagram with the step STEP_ID as second of three steps, and a loop as fourth with one step inside, which is 4.1. */
  private static File diagramWithSteps() {
    return diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first"))
        .withStep(simple("second").withId(STEP_ID))
        .withStep(simple("third"))
        .withStep(whileStep("loop").withLoopSequence(sequence().withStep(simple("inner").withId("1ab2c3d4")))))
      .persist();
  }

  private static String path(File file) {
    return LinkAliases.absolutePath(file);
  }

  private static String line(String status, File file, String detail) {
    return status + TAB + (file == null ? "" : path(file)) + TAB + detail;
  }

  private static List<String> linesOf(CLIResult result) {
    if (result.stdout.isEmpty()) {
      return List.of();
    }
    assertTrue(result.stdout.endsWith("\n"), "every line ends with a line feed: " + result);
    return List.of(result.stdout.substring(0, result.stdout.length() - 1).split("\n", -1));
  }

  /** A link to a file without step is fine if the file exists. The result tells the file and repeats the link. */
  @Test
  void testLinkToFileIsOK() {
    File file = diagramWithSteps();
    String link = SpecmanLink.of(file, "", null, null);

    CLIResult result = resolveLinks(link);

    assertEquals(List.of(line("OK", file, link)), linesOf(result));
    assertEquals(0, result.exitState);
  }

  /** The alias is resolved with the user's aliases, and the link stays in the form with the alias. */
  @Test
  void testAliasIsResolved() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto");

    assertEquals(List.of(line("OK", file, "specman://auto")), linesOf(result));
  }

  /** A link with a step doesn't need the step number, but the result tells it. */
  @Test
  void testStepWithoutNumberIsCompletedWithNumber() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto," + STEP_ID);

    assertEquals(List.of(line("OK", file, "specman://auto," + STEP_ID + ",2")), linesOf(result));
  }

  /** A step with the right number is just as OK, steps inside other steps included. */
  @Test
  void testStepWithRightNumberIsOK() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto," + STEP_ID + ",2", "specman://auto,1ab2c3d4,4.1");

    assertEquals(List.of(
      line("OK", file, "specman://auto," + STEP_ID + ",2"),
      line("OK", file, "specman://auto,1ab2c3d4,4.1")), linesOf(result));
    assertEquals(0, result.exitState);
  }

  /** Scenario: The step was moved, so its number changed. The link is reported as sanitized, with the corrected link
   * which keeps the alias. This is no error, the step is still found by its ID. */
  @Test
  void testWrongNumberIsSanitized() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto," + STEP_ID + ",7.7");

    assertEquals(List.of(line("SANITIZED", file, "specman://auto," + STEP_ID + ",2")), linesOf(result));
    assertEquals(0, result.exitState);
  }

  /** Without an alias, the link names the file by its path, which is the form the corrected link keeps as well. */
  @Test
  void testLinkWithPathKeepsPath() {
    File file = diagramWithSteps();
    String correctLink = SpecmanLink.of(file, "", STEP_ID, "2");
    String wrongLink = SpecmanLink.of(file, "", STEP_ID, "5");

    CLIResult result = resolveLinks(wrongLink);

    assertEquals(List.of(line("SANITIZED", file, correctLink)), linesOf(result));
  }

  /** Scenario: The step doesn't exist (anymore). The file is told nevertheless, it's the step which is missing. */
  @Test
  void testUnknownStepIsAnError() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto,deadbeef,2");

    assertEquals(List.of(line("ERROR", file, "Step deadbeef doesn't exist in the file")), linesOf(result));
    assertEquals(1, result.exitState);
  }

  /** Scenario: The alias isn't assigned to a file. There is no file to tell. */
  @Test
  void testUnknownAliasIsAnError() {
    CLIResult result = resolveLinks("specman://auto," + STEP_ID);

    assertEquals(List.of(line("ERROR", null, "Unknown alias: auto")), linesOf(result));
    assertEquals(1, result.exitState);
  }

  /** Scenario: The file the link or alias points to is gone. */
  @Test
  void testMissingFileIsAnError() {
    File file = new File("does-not-exist.nsd");
    String link = SpecmanLink.of(file, "", STEP_ID, "2");

    CLIResult result = resolveLinks(link);

    assertEquals(List.of(line("ERROR", file, "File not found")), linesOf(result));
  }

  /** Scenario: The file exists but isn't a diagram. Without a step the file is all that counts, with a step it must be read. */
  @Test
  void testFileWhichIsNoDiagramIsAnErrorForStepsOnly(@TempDir Path directory) throws IOException {
    File file = directory.resolve("notes.nsd").toFile();
    Files.writeString(file.toPath(), "no diagram at all");
    LinkAliases.assign(file, "notes");

    CLIResult result = resolveLinks("specman://notes", "specman://notes," + STEP_ID);

    List<String> lines = linesOf(result);
    assertEquals(2, lines.size());
    assertEquals(line("OK", file, "specman://notes"), lines.get(0));
    assertTrue(lines.get(1).startsWith("ERROR" + TAB + path(file) + TAB + "The file can't be read: "), lines.get(1));
  }

  /** Whatever isn't a link gets its line as well, so the lines always match the links one to one. */
  @Test
  void testNoLinkIsAnErrorWithItsOwnLine() {
    CLIResult result = resolveLinks("hello", "", "https://specman.org", "specman://");

    assertEquals(List.of(
      line("ERROR", null, "Not a Specman link: hello"),
      line("ERROR", null, "Not a Specman link: "),
      line("ERROR", null, "Not a Specman link: https://specman.org"),
      line("ERROR", null, "Not a Specman link: specman://")), linesOf(result));
    assertEquals(1, result.exitState);
  }

  /** Scenario: Several links in one call are answered in their order, a failing one doesn't stop the others and decides the
   * exit state. */
  @Test
  void testSeveralLinksKeepTheirOrder() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinks("specman://auto,deadbeef", "specman://auto," + STEP_ID + ",1", "specman://auto");

    assertEquals(List.of(
      line("ERROR", file, "Step deadbeef doesn't exist in the file"),
      line("SANITIZED", file, "specman://auto," + STEP_ID + ",2"),
      line("OK", file, "specman://auto")), linesOf(result));
    assertEquals(1, result.exitState);
  }

  /** Scenario: Another tool pipes the links in, one per line. Without links as arguments they are read from stdin. */
  @Test
  void testLinksFromStdin() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, "auto");

    CLIResult result = resolveLinksFromStdin("specman://auto," + STEP_ID + ",2\r\n\nspecman://nobody\n");

    assertEquals(List.of(
      line("OK", file, "specman://auto," + STEP_ID + ",2"),
      line("ERROR", null, "Not a Specman link: "),
      line("ERROR", null, "Unknown alias: nobody")), linesOf(result));
  }

  /** Scenario: Nothing to check at all ends without output and successfully. */
  @Test
  void testNoLinksAtAllWriteNothing() {
    CLIResult result = resolveLinksFromStdin("");

    assertEquals(List.of(), linesOf(result));
    assertEquals(0, result.exitState);
  }

  /** Scenario: A path with umlauts comes out as UTF-8, whatever the console's code page is. */
  @Test
  void testNonAsciiPathIsWrittenAsUTF8(@TempDir Path directory) throws IOException {
    File source = diagramWithSteps();
    File file = directory.resolve("Übersicht für Geschäft.nsd").toFile();
    Files.copy(source.toPath(), file.toPath());
    String link = SpecmanLink.of(file, "", STEP_ID, "2");

    CLIResult result = resolveLinks(link);

    assertEquals(List.of(line("OK", file, link)), linesOf(result));
  }

  /** The three columns of a line never contain line breaks or tabs of their own, which would break the pipes. */
  @Test
  void testExplanationsStayInOneLine() {
    CLIResult result = resolveLinks("specman://a\tb\nc");

    assertEquals(1, linesOf(result).size());
    assertEquals(3, linesOf(result).get(0).split(TAB, -1).length);
  }
}
