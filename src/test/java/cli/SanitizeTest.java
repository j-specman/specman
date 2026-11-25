package cli;

import builder.DiagrammBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import specman.Aenderungsart;
import specman.editarea.markups.MarkupType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static builder.BuilderFactory.source;
import static cli.SpecmanCLIFT.call;
import static cli.SpecmanCLIFT.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/** Tests of the {@code --sanitize} command line operation. */
public class SanitizeTest {

  private static final String TAG_WITH_FREETEXT = "priority";
  private static final String TAG_WITHOUT_FREETEXT = "done";
  private static final String UNKNOWN_TAG = "unknown";
  private static final String INVISIBLE_TAG = "invisible";
  private static final String TEXT = "Das System prüft die Eingabe.";
  private static final String CHANGESET = "yellow";
  private static final String OTHER_CHANGESET = "pink";
  private static final String UNKNOWN_CHANGESET = "nosuchcolor";
  private static final String SOURCE_ID = "a1b2c3d4";
  private static final String TARGET_ID = "c3d4e5f6";
  private static final String OTHER_STEP_ID = "0badf00d";
  private static final String UNKNOWN_STEP_ID = "deadbeef";
  private static final List<String> SPECMAN_WRITTEN_FILES = List.of(
    "specman-reference-v1.4-model-v2.nsd", "doc/changetracking.nsd", "doc/breakcatch.nsd");

  @BeforeAll
  static void deleteOldNsdFiles() throws IOException {
    DiagrammBuilder.deleteOldNsdFiles();
  }

  /** A model file which is already consistent is reported as OK and the call ends successfully. */
  @Test
  void testMinimalOKCase() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertOK(result);
  }

  /** A file which doesn't exist is reported as ERROR on stderr and the call ends with exit state 1. */
  @Test
  void testMissingFile() {
    String UNKNOWN_FILE = "does-not-exist.nsd";
    CLIResult result = sanitize(new File(UNKNOWN_FILE));
    assertERROR(result, UNKNOWN_FILE);
  }

  /** Two steps with the same id can't be repaired automatically, so they are reported as ERROR including the id. */
  @Test
  void testDuplicateStepIds() {
    String DUBLICATE_ID = "12345678";
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withId(DUBLICATE_ID))
        .withStep(simple("second").withId(DUBLICATE_ID)))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Duplicate step IDs", DUBLICATE_ID);
  }

  /** Artificial step numbers are replaced by the consecutive ones, the changes are reported and the file is repaired. */
  @Test
  void testWrongStepNumbersAreRepaired() {
    File nsdFile = diagramm()
      .withRenumberSteps(false)
      .withMainSequence(sequence()
        .withStep(simple().withStepNumber("1x"))
        .withStep(simple().withStepNumber("2"))
        .withStep(simple().withStepNumber("3x")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertSANITIZED(result,
      "Step numbers changed (2)",
      "1x -> 1",
      "3x -> 3");

    assertOK(sanitize(nsdFile));
  }

  /** An outdated plain text rendition is refreshed from the text, the refresh is reported and the file is repaired. */
  @Test
  void testOutdatedPlainTextIsRefreshed() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withPlainText("outdated")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertSANITIZED(result, "Plain text fields refreshed: 1");

    assertOK(sanitize(nsdFile));
  }

  /** A step link follows the renumbering of the referenced step, the update is reported and the file is repaired. */
  @Test
  void testStepLinkFollowsRenumberedStep() throws IOException {
    File nsdFile = diagramm()
      .withRenumberSteps(false)
      .withMainSequence(sequence()
        .withStep(simple("first").withStepNumber("8"))
        .withStep(simple("see step 8").withSteplink("8").withStepNumber("2")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertSANITIZED(result, "Steplink references updated (1)", "8 -> 1");
    assertContains(Files.readString(nsdFile.toPath()), "`see step 1`");
    assertOK(sanitize(nsdFile));
  }

  /** A step link to a step number which doesn't exist can't be repaired and is reported as ERROR. */
  @Test
  void testStepLinkToUnknownStep() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first"))
        .withStep(simple("see step 9").withSteplink("9")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Unresolvable steplink references", "9");
  }

  /** A file with a syntax error is reported as ERROR. The error is made by deleting the semicolons behind the steps. */
  @Test
  void testSyntaxError() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first")))
      .persist();
    removeFromFile(nsdFile, ";");

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "mismatched input '}'");
  }

  /** Without a file argument the call is reported as ERROR with a usage hint. */
  @Test
  void testMissingFileArgument() {
    CLIResult result = call("--sanitize");

    assertERROR(result, "Usage");
  }

  /** A consistent file is not touched, so its modification can't trigger anything like a file watcher. */
  @Test
  void testFileOfOKCaseStaysUnchanged() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first")))
      .persist();
    String contentBefore = Files.readString(nsdFile.toPath());

    sanitize(nsdFile);

    assertEquals(contentBefore, Files.readString(nsdFile.toPath()));
  }

  /** A file with an error which can't be repaired is not written, even if it contains repairable errors as well. */
  @Test
  void testFileWithUnresolvableStepLinkStaysUnchanged() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withPlainText("outdated"))
        .withStep(simple("see step 9").withSteplink("9")))
      .persist();
    String contentBefore = Files.readString(nsdFile.toPath());

    sanitize(nsdFile);

    assertEquals(contentBefore, Files.readString(nsdFile.toPath()));
  }

  /** Tags which refer to a configuration and respect its freetext setting are accepted. */
  @Test
  void testValidMetaTagsAreAccepted() {
    File nsdFile = diagramm()
      .withMetaTagConfig(TAG_WITH_FREETEXT, "Priority", true)
      .withMetaTagConfig(TAG_WITHOUT_FREETEXT, "Done", false)
      .withMainSequence(sequence()
        .withStep(simple("first")
          .withTag(TAG_WITH_FREETEXT, "high")
          .withTag(TAG_WITHOUT_FREETEXT)))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertOK(result);
  }

  /** A tag which refers to a configuration that doesn't exist is reported as ERROR including the tag's name and step. */
  @Test
  void testTagWithoutConfiguration() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withTag(UNKNOWN_TAG)))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid meta tags", "Step 1", "'" + UNKNOWN_TAG + "' has no configuration");
  }

  /** A step with the same tag twice is reported as ERROR including the tag's name. */
  @Test
  void testTagAssignedTwiceToStep() {
    File nsdFile = diagramm()
      .withMetaTagConfig(TAG_WITH_FREETEXT, "Priority", true)
      .withMainSequence(sequence()
        .withStep(simple("first")
          .withTag(TAG_WITH_FREETEXT, "high")
          .withTag(TAG_WITH_FREETEXT, "low")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid meta tags", "'" + TAG_WITH_FREETEXT + "' is assigned more than once");
  }

  /** A tag with freetext at a configuration which doesn't allow freetext is reported as ERROR. */
  @Test
  void testFreetextAtTagWithoutFreetextAllowed() {
    File nsdFile = diagramm()
      .withMetaTagConfig(TAG_WITHOUT_FREETEXT, "Done", false)
      .withMainSequence(sequence()
        .withStep(simple("first").withTag(TAG_WITHOUT_FREETEXT, "unexpected")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid meta tags", "'" + TAG_WITHOUT_FREETEXT + "' has freetext");
  }

  /** A configuration which shows neither label nor icon is reported as ERROR including the configuration's name. */
  @Test
  void testTagConfigurationWithoutLabelAndIcon() {
    File nsdFile = diagramm()
      .withMetaTagConfig(INVISIBLE_TAG, "", true)
      .withMainSequence(sequence()
        .withStep(simple("first")))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid meta tags", "'" + INVISIBLE_TAG + "' has neither label nor icon");
  }

  // ---- stereotypes in class diagrams ----

  private static String diagramWithStereotype(String configName) {
    return "classdiagram {\n    class Customer (aaaaaaaa) <<@" + configName + ">>\n}\n";
  }

  /** A class diagram whose stereotypes refer to configurations which exist is accepted. */
  @Test
  void testStereotypeOfKnownConfigurationIsAccepted() {
    File nsdFile = diagramm()
      .withMetaTagConfig(TAG_WITH_FREETEXT, "Priority", true)
      .withMainSequence(sequence()
        .withStep(simple("first").withClassmanDiagram(diagramWithStereotype(TAG_WITH_FREETEXT))))
      .persist();

    assertOK(sanitize(nsdFile));
  }

  /** A stereotype which refers to a configuration that doesn't exist is reported as ERROR including the class, the
   * configuration and the step with the diagram. */
  @Test
  void testStereotypeWithoutConfiguration() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withClassmanDiagram(diagramWithStereotype(UNKNOWN_TAG))))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid meta tags", "Step 1", "class 'Customer'", "'" + UNKNOWN_TAG + "' which has no configuration");
  }

  /** A diagram in the cell of a table is looked at as well. */
  @Test
  void testStereotypeWithoutConfigurationInTableCell() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withClassmanDiagramInTableCell(diagramWithStereotype(UNKNOWN_TAG))))
      .persist();

    assertERROR(sanitize(nsdFile), "Invalid meta tags", "Step 1", "table cell", "'" + UNKNOWN_TAG + "'");
  }

  /** A diagram in the intro is looked at as well. */
  @Test
  void testStereotypeWithoutConfigurationInIntro() {
    File nsdFile = diagramm()
      .withIntroDiagram(diagramWithStereotype(UNKNOWN_TAG))
      .withMainSequence(sequence()
        .withStep(simple("first")))
      .persist();

    assertERROR(sanitize(nsdFile), "Invalid meta tags", "intro", "'" + UNKNOWN_TAG + "'");
  }

  /** Tags at steps and stereotypes are reported together. */
  @Test
  void testTagAndStereotypeWithoutConfigurationAreReportedTogether() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withTag(UNKNOWN_TAG).withClassmanDiagram(diagramWithStereotype(UNKNOWN_TAG))))
      .persist();

    assertERROR(sanitize(nsdFile), "Invalid meta tags", "tag '" + UNKNOWN_TAG + "' has no configuration",
      "class 'Customer'");
  }

  /** The old text of a change mark may refer to a configuration which doesn't exist, which is no use of it. */
  @Test
  void testBackupOfChangeMarkMayBeDangling() {
    String source = "classdiagram {\n    class Customer (aaaaaaaa) <<Entity>> was(yellow, \"@" + UNKNOWN_TAG + "\")\n}\n";
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withClassmanDiagram(source)))
      .persist();

    assertOK(sanitize(nsdFile));
  }

  // ---- change tracking ----

  /** Markups covering the text up to its last character, one after the other, are accepted. */
  @Test
  void testMarkupsWithinTextAreAccepted() {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT)
          .withMarkupAt(0, 2, MarkupType.Changed, CHANGESET)
          .withMarkupAt(4, TEXT.length() - 1, MarkupType.Changed, CHANGESET)))
      .persist();

    CLIResult result = sanitize(nsdFile);

    assertOK(result);
  }

  /** The end of a markup is the last marked character, so the length of the text itself is already outside. The message
   * names the line the markup is in, and the plain text the offsets refer to. */
  @Test
  void testMarkupEndingBehindTheText() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withMarkupAt(0, TEXT.length(), MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "line " + lineOf(nsdFile, "`" + TEXT + "`") + ":",
      "markup (0," + TEXT.length() + ")", "lies outside", "length 29, last index 28", "`" + TEXT + "`");
  }

  /** A markup far behind the end of the text is reported as ERROR. */
  @Test
  void testMarkupFarBehindTheText() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withMarkupAt(0, 500, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "markup (0,500)", "lies outside");
  }

  /** A markup which ends before it starts is reported as ERROR. */
  @Test
  void testMarkupEndingBeforeItsStart() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withMarkupAt(10, 3, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "markup (10,3)", "ends before it starts");
  }

  /** Markups which overlap are reported as ERROR. */
  @Test
  void testOverlappingMarkups() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT)
          .withMarkupAt(0, 5, MarkupType.Changed, CHANGESET)
          .withMarkupAt(5, 9, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "markup (5,9)", "overlaps or precedes", "(0,5)");
  }

  /** Markups which are not in ascending order are reported as ERROR. */
  @Test
  void testMarkupsInWrongOrder() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT)
          .withMarkupAt(10, 12, MarkupType.Changed, CHANGESET)
          .withMarkupAt(0, 3, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "markup (0,3)", "overlaps or precedes", "(10,12)");
  }

  /** A markup which names a change set that doesn't exist is reported as ERROR including the known change sets. */
  @Test
  void testMarkupWithUnknownChangeset() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withMarkupAt(0, 5, MarkupType.Changed, CHANGESET)))
      .persist();
    replaceInFile(nsdFile, "Changed," + CHANGESET, "Changed," + UNKNOWN_CHANGESET);

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "line " + lineOf(nsdFile, "`" + TEXT + "`") + ":",
      "unknown changeset '" + UNKNOWN_CHANGESET + "'", "yellow, lilac, blue, orange, pink");
  }

  /** A markup of a change without change set is reported as ERROR. */
  @Test
  void testMarkupOfChangeWithoutChangeset() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withMarkupAt(0, 5, MarkupType.Changed, null)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "names no changeset");
  }

  /** Markups in list items and table cells are checked like those of the step's text. */
  @Test
  void testMarkupsInListItemsAndTableCells() throws IOException {
    File listFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withListItemMarkup(TEXT, 0, 500, MarkupType.Changed, CHANGESET)))
      .persist();
    File tableFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withTableCellMarkup(TEXT, 0, 500, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(listFile, "Invalid change tracking", "list item", "markup (0,500)");
    assertERRORAndFileUnchanged(tableFile, "Invalid change tracking", "table cell", "markup (0,500)");
  }

  /** The check of the markups uses the plain text as the sanitizer renders it, not the outdated one in the file. */
  @Test
  void testMarkupIsCheckedAgainstRegeneratedPlainText() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withPlainText("outdated plain text, much longer than the text itself")
          .withMarkupAt(30, 40, MarkupType.Changed, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "markup (30,40)", "length 29");
  }

  /** A change which names a change set that doesn't exist is reported as ERROR with its line, and not as an exception. */
  @Test
  void testChangeWithUnknownChangeset() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withChange(Aenderungsart.Hinzugefuegt, CHANGESET)))
      .persist();
    replaceInFile(nsdFile, "change=(added, " + CHANGESET + ")", "change=(added, " + UNKNOWN_CHANGESET + ")");

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid change tracking", "line " + lineOf(nsdFile, "change=(added") + ":",
      "Unknown changeset '" + UNKNOWN_CHANGESET + "'", "change=(added", "yellow, lilac, blue, orange, pink");
    assertFalse(result.stderr.contains("Cannot invoke"), result.toString());
  }

  /** A change type which doesn't exist is a syntax error with line and column. */
  @Test
  void testUnknownChangeType() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withChange(Aenderungsart.Hinzugefuegt, CHANGESET)))
      .persist();
    replaceInFile(nsdFile, "change=(added", "change=(moved");

    assertERRORAndFileUnchanged(nsdFile, "line " + lineOf(nsdFile, "change=(moved") + ":");
  }

  /** A moved step is accepted if source and target step refer to each other and share their change set. */
  @Test
  void testMovedStepIsAccepted() {
    File nsdFile = movedStepFile(SOURCE_ID, TARGET_ID, CHANGESET, CHANGESET);

    CLIResult result = sanitize(nsdFile);

    assertOK(result);
  }

  /** A target step which refers to a step that doesn't exist is reported as ERROR. */
  @Test
  void testTargetStepRefersToUnknownStep() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withId(TARGET_ID).withChange(Aenderungsart.Zielschritt, CHANGESET)
          .withSourceStepId(UNKNOWN_STEP_ID)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "line " + lineOf(nsdFile, "change=(target") + ":",
      "sourceStep=" + UNKNOWN_STEP_ID, "doesn't exist");
  }

  /** A target step without reference to its source step is reported as ERROR. */
  @Test
  void testTargetStepWithoutSourceStep() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withId(TARGET_ID).withChange(Aenderungsart.Zielschritt, CHANGESET)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "target step without sourceStep=");
  }

  /** A target step must refer to a source step, not to just any step. */
  @Test
  void testTargetStepRefersToStepWhichIsNoSourceStep() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withId(SOURCE_ID))
        .withStep(simple(TEXT).withId(TARGET_ID).withChange(Aenderungsart.Zielschritt, CHANGESET)
          .withSourceStepId(SOURCE_ID)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "which is not a source step");
  }

  /** A source step must refer back to the target step referring to it. */
  @Test
  void testSourceStepRefersToOtherStep() throws IOException {
    File nsdFile = movedStepFile(OTHER_STEP_ID, TARGET_ID, CHANGESET, CHANGESET);

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "is no counterpart", OTHER_STEP_ID);
  }

  /** A source step without reference to its target step is reported as ERROR. */
  @Test
  void testSourceStepWithoutTargetStep() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(source().withId(SOURCE_ID))
        .withStep(simple(TEXT)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "source step without sourceStep=");
  }

  /** A source step which refers to a step that doesn't exist is reported as ERROR. */
  @Test
  void testSourceStepRefersToUnknownStep() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(source().withId(SOURCE_ID).withSourceStepId(UNKNOWN_STEP_ID))
        .withStep(simple(TEXT)))
      .persist();

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "sourceStep=" + UNKNOWN_STEP_ID, "doesn't exist");
  }

  /** The reference to a source step is only allowed at target steps. */
  @Test
  void testSourceStepReferenceAtStepWhichIsNoTarget() throws IOException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple(TEXT).withChange(Aenderungsart.Hinzugefuegt, CHANGESET)))
      .persist();
    replaceInFile(nsdFile, "change=(added, " + CHANGESET + ")",
      "change=(added, " + CHANGESET + ", sourceStep=" + UNKNOWN_STEP_ID + ")");

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "only allowed at target steps");
  }

  /** Source and target step of a moved step must belong to the same change set. */
  @Test
  void testMovedStepWithDifferentChangesets() throws IOException {
    File nsdFile = movedStepFile(SOURCE_ID, TARGET_ID, "lilac", CHANGESET);

    assertERRORAndFileUnchanged(nsdFile, "Invalid change tracking", "same changeset", "'lilac'", "'" + CHANGESET + "'");
  }

  /** The change sets of source steps and of the backup of a PlantUML diagram are checked as well. */
  @Test
  void testUnknownChangesetAtSourceStepAndBackup() throws IOException {
    File nsdFile = movedStepFile(SOURCE_ID, TARGET_ID, OTHER_CHANGESET, OTHER_CHANGESET);
    replaceInFile(nsdFile, "sourceStep=" + TARGET_ID + ");", "sourceStep=" + TARGET_ID + ")"
      + "{ image(scale=100%, type=plantuml) { `@startuml@enduml`, backup=(" + OTHER_CHANGESET + ", `old`) } }");
    replaceInFile(nsdFile, OTHER_CHANGESET, UNKNOWN_CHANGESET);

    CLIResult result = sanitize(nsdFile);

    assertERROR(result, "Invalid change tracking", "in source(...)", "in change=(target", "in backup=(...)");
  }

  /** Files written by Specman itself, with every kind of change tracking, are not rejected by the checks. */
  @Test
  void testFilesWrittenBySpecmanAreAccepted() throws IOException {
    for (String original : SPECMAN_WRITTEN_FILES) {
      File copy = Path.of("target", "nsd-tests", "copy-of-" + Path.of(original).getFileName()).toAbsolutePath().toFile();
      Files.copy(Path.of(original), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);

      CLIResult result = sanitize(copy);

      assertEquals(EXIT_SUCCESS, result.exitState, original + ": " + result);
    }
  }

  private static File movedStepFile(String sourceStepReference, String targetStepReference, String sourceChangeset,
                                    String targetChangeset) {
    return diagramm()
      .withMainSequence(sequence()
        .withStep(source().withId(SOURCE_ID).withChange(Aenderungsart.Quellschritt, sourceChangeset)
          .withSourceStepId(targetStepReference))
        .withStep(simple(TEXT).withId(TARGET_ID).withChange(Aenderungsart.Zielschritt, targetChangeset)
          .withSourceStepId(sourceStepReference)))
      .persist();
  }

  private static void replaceInFile(File file, String fragment, String replacement) throws IOException {
    Path path = file.toPath();
    String content = Files.readString(path);
    assertTrue(content.contains(fragment), "'" + fragment + "' not found in '" + content + "'");
    Files.writeString(path, content.replace(fragment, replacement));
  }

  /** @return the number of the first line containing the fragment, counting from 1 like the messages do */
  private static int lineOf(File file, String fragment) throws IOException {
    List<String> lines = Files.readAllLines(file.toPath());
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains(fragment)) {
        return i + 1;
      }
    }
    throw new AssertionError("'" + fragment + "' not found in " + file);
  }

  /** A file with an error is reported as ERROR with the expected fragments and is not written, however
   * much the sanitizer would have corrected otherwise. */
  private void assertERRORAndFileUnchanged(File file, String... fragments) throws IOException {
    String contentBefore = Files.readString(file.toPath());

    CLIResult result = sanitize(file);

    assertERROR(result, fragments);
    assertEquals(contentBefore, Files.readString(file.toPath()));
  }

  private static void removeFromFile(File file, String fragment) throws IOException {
    Path path = file.toPath();
    Files.writeString(path, Files.readString(path).replace(fragment, ""));
  }

  private void assertSANITIZED(CLIResult result, String... fragments) {
    assertEquals(EXIT_SUCCESS, result.exitState, result.toString());
    assertContains(result.stdout, "SANITIZED");
    for (String fragment : fragments) {
      assertContains(result.stdout, fragment);
    }
  }

  private void assertERROR(CLIResult result, String... fragments) {
    assertEquals(EXIT_ERROR, result.exitState, result.toString());
    assertContains(result.stderr, "ERROR");
    for (String fragment : fragments) {
      assertContains(result.stderr, fragment);
    }
  }

  private void assertContains(String content, String fragment) {
    assertTrue(content.contains(fragment), "Expected fragment '" + fragment + "' not found in '" + content + "'");
  }

  private void assertOK(CLIResult result) {
    assertEquals(EXIT_SUCCESS, result.exitState, result.toString());
    assertContains(result.stdout, "OK");
  }

}
