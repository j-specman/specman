package cli;

import builder.DiagrammBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.cli.ResolveLinkCLO;
import specman.link.LinkAliases;
import specman.link.SpecmanLink;
import specman.link.resolver.ResolutionStatus;

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
import static specman.cli.ResolveLinkCLO.FIELD_SEPARATOR;
import static specman.cli.ResolveLinkCLO.LINE_END;
import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;
import static specman.link.SpecmanLink.SEPARATOR;
import static specman.link.resolver.ResolutionStatus.ERROR;
import static specman.link.resolver.ResolutionStatus.OK;
import static specman.link.resolver.ResolutionStatus.SANITIZED;

/** Tests of the {@code --resolve-link} command line operation: For each link exactly one line of
 * status, file and detail, separated by tabs. */
public class ResolveLinkTest {

  private static final String ALIAS = "uc100";
  private static final String ALIAS_LINK = SpecmanLink.SCHEME + ALIAS;
  private static final String UNKNOWN_ALIAS = "uc999";
  private static final String STEP_ID = "1e76071a";
  private static final String STEP_NUMBER = "2";
  private static final String INNER_STEP_ID = "1ab2c3d4";
  private static final String INNER_STEP_NUMBER = "4.1";
  private static final String WRONG_STEP_NUMBER = "7.7";
  private static final String MISSING_STEP_ID = "deadbeef";
  private static final String MISSING_STEP_MESSAGE = "Step " + MISSING_STEP_ID + " doesn't exist in the file";
  private static final String UNKNOWN_ALIAS_MESSAGE = "Unknown alias: ";
  private static final String NO_LINK_MESSAGE = "Not a Specman link: ";

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
        .withStep(whileStep("loop").withLoopSequence(sequence().withStep(simple("inner").withId(INNER_STEP_ID)))))
      .persist();
  }

  /** The diagram with the alias assigned to it. */
  private static File diagramWithAlias() {
    File file = diagramWithSteps();
    LinkAliases.assign(file, ALIAS);
    return file;
  }

  private static String aliasLink(String stepId) {
    return ALIAS_LINK + SEPARATOR + stepId;
  }

  private static String aliasLink(String stepId, String stepNumber) {
    return aliasLink(stepId) + SEPARATOR + stepNumber;
  }

  private static String path(File file) {
    return LinkAliases.absolutePath(file);
  }

  private static String line(ResolutionStatus status, File file, String detail) {
    return status + FIELD_SEPARATOR + (file == null ? "" : path(file)) + FIELD_SEPARATOR + detail;
  }

  /** Checks that the call wrote exactly the expected lines, one for each link. */
  private static void assertResult(CLIResult result, String... expectedLines) {
    assertEquals(List.of(expectedLines), linesOf(result), result.toString());
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

    assertResult(result, line(OK, file, link));
    assertEquals(EXIT_SUCCESS, result.exitState);
  }

  /** The alias is resolved with the user's aliases, and the link stays in the form with the alias. */
  @Test
  void testAliasIsResolved() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(ALIAS_LINK);

    assertResult(result, line(OK, file, ALIAS_LINK));
  }

  /** A link with a step doesn't need the step number, but the result tells it. */
  @Test
  void testStepWithoutNumberIsCompletedWithNumber() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(aliasLink(STEP_ID));

    assertResult(result, line(OK, file, aliasLink(STEP_ID, STEP_NUMBER)));
  }

  /** A step with the right number is just as OK, steps inside other steps included. */
  @Test
  void testStepWithRightNumberIsOK() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(aliasLink(STEP_ID, STEP_NUMBER), aliasLink(INNER_STEP_ID, INNER_STEP_NUMBER));

    assertResult(result,
      line(OK, file, aliasLink(STEP_ID, STEP_NUMBER)),
      line(OK, file, aliasLink(INNER_STEP_ID, INNER_STEP_NUMBER)));
    assertEquals(EXIT_SUCCESS, result.exitState);
  }

  /** Scenario: The step was moved, so its number changed. The link is reported as sanitized, with the corrected link
   * which keeps the alias. This is no error, the step is still found by its ID. */
  @Test
  void testWrongNumberIsSanitized() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(aliasLink(STEP_ID, WRONG_STEP_NUMBER));

    assertResult(result, line(SANITIZED, file, aliasLink(STEP_ID, STEP_NUMBER)));
    assertEquals(EXIT_SUCCESS, result.exitState);
  }

  /** Without an alias, the link names the file by its path, which is the form the corrected link keeps as well. */
  @Test
  void testLinkWithPathKeepsPath() {
    File file = diagramWithSteps();
    String correctLink = SpecmanLink.of(file, "", STEP_ID, STEP_NUMBER);
    String wrongLink = SpecmanLink.of(file, "", STEP_ID, WRONG_STEP_NUMBER);

    CLIResult result = resolveLinks(wrongLink);

    assertResult(result, line(SANITIZED, file, correctLink));
  }

  /** Scenario: The step doesn't exist (anymore). The file is told nevertheless, it's the step which is missing. */
  @Test
  void testUnknownStepIsAnError() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(aliasLink(MISSING_STEP_ID, STEP_NUMBER));

    assertResult(result, line(ERROR, file, MISSING_STEP_MESSAGE));
    assertEquals(EXIT_ERROR, result.exitState);
  }

  /** Scenario: The alias isn't assigned to a file. There is no file to tell. */
  @Test
  void testUnknownAliasIsAnError() {
    CLIResult result = resolveLinks(SpecmanLink.SCHEME + UNKNOWN_ALIAS + SEPARATOR + STEP_ID);

    assertResult(result,
      line(ERROR, null, UNKNOWN_ALIAS_MESSAGE + UNKNOWN_ALIAS));
    assertEquals(EXIT_ERROR, result.exitState);
  }

  /** Scenario: The file the link or alias points to is gone. */
  @Test
  void testMissingFileIsAnError() {
    File file = new File("does-not-exist.nsd");
    String link = SpecmanLink.of(file, "", STEP_ID, STEP_NUMBER);

    CLIResult result = resolveLinks(link);

    assertResult(result, line(ERROR, file, "File not found"));
  }

  /** Scenario: The file exists but isn't a diagram. Without a step the file is all that counts, with a step it must be read. */
  @Test
  void testFileWhichIsNoDiagramIsAnErrorForStepsOnly(@TempDir Path directory) throws IOException {
    File file = directory.resolve("notes.nsd").toFile();
    Files.writeString(file.toPath(), "no diagram at all");
    LinkAliases.assign(file, ALIAS);

    CLIResult result = resolveLinks(ALIAS_LINK, aliasLink(STEP_ID));

    List<String> lines = linesOf(result);
    assertEquals(2, lines.size());
    assertEquals(line(OK, file, ALIAS_LINK), lines.get(0));
    assertTrue(lines.get(1).startsWith(ERROR + FIELD_SEPARATOR + path(file) + FIELD_SEPARATOR + "The file can't be read: "), lines.get(1));
  }

  /** Whatever isn't a link gets its line as well, so the lines always match the links one to one. */
  @Test
  void testNoLinkIsAnErrorWithItsOwnLine() {
    CLIResult result = resolveLinks("hello", "", "http://specman.de", SpecmanLink.SCHEME);

    assertResult(result,
      line(ERROR, null, NO_LINK_MESSAGE + "hello"),
      line(ERROR, null, NO_LINK_MESSAGE),
      line(ERROR, null, NO_LINK_MESSAGE + "http://specman.de"),
      line(ERROR, null, NO_LINK_MESSAGE + SpecmanLink.SCHEME));
    assertEquals(EXIT_ERROR, result.exitState);
  }

  /** Scenario: Several links in one call are answered in their order, a failing one doesn't stop the others and decides the
   * exit state. */
  @Test
  void testSeveralLinksKeepTheirOrder() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinks(
      aliasLink(MISSING_STEP_ID),
      aliasLink(STEP_ID, WRONG_STEP_NUMBER),
      ALIAS_LINK);

    assertResult(result,
      line(ERROR, file, MISSING_STEP_MESSAGE),
      line(SANITIZED, file, aliasLink(STEP_ID, STEP_NUMBER)),
      line(OK, file, ALIAS_LINK));
    assertEquals(EXIT_ERROR, result.exitState);
  }

  /** Scenario: Another tool pipes the links in, one per line. Without links as arguments they are read from stdin. */
  @Test
  void testLinksFromStdin() {
    File file = diagramWithAlias();

    CLIResult result = resolveLinksFromStdin(
      aliasLink(STEP_ID, STEP_NUMBER) + "\r\n\n" + SpecmanLink.SCHEME + UNKNOWN_ALIAS + "\n");

    assertResult(result,
      line(OK, file, aliasLink(STEP_ID, STEP_NUMBER)),
      line(ERROR, null, NO_LINK_MESSAGE),
      line(ERROR, null, UNKNOWN_ALIAS_MESSAGE + UNKNOWN_ALIAS));
  }

  /** Scenario: Nothing to check at all ends without output and successfully. */
  @Test
  void testNoLinksAtAllWriteNothing() {
    CLIResult result = resolveLinksFromStdin("");

    assertResult(result);
    assertEquals(EXIT_SUCCESS, result.exitState);
  }

  /** Scenario: A path with umlauts comes out as UTF-8, whatever the console's code page is. */
  @Test
  void testNonAsciiPathIsWrittenAsUTF8(@TempDir Path directory) throws IOException {
    File source = diagramWithSteps();
    File file = directory.resolve("Übersicht für Geschäft.nsd").toFile();
    Files.copy(source.toPath(), file.toPath());
    String link = SpecmanLink.of(file, "", STEP_ID, STEP_NUMBER);

    CLIResult result = resolveLinks(link);

    assertResult(result, line(OK, file, link));
  }

  /** The three columns of a line never contain line breaks or tabs of their own, which would break the pipes. */
  @Test
  void testExplanationsStayInOneLine() {
    CLIResult result = resolveLinks(SpecmanLink.SCHEME + "a" + FIELD_SEPARATOR + "b" + LINE_END + "c");

    assertEquals(1, linesOf(result).size());
    assertEquals(3, linesOf(result).get(0).split(FIELD_SEPARATOR, -1).length);
  }
}
