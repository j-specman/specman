package cli;

import builder.DiagrammBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static cli.SpecmanCLIFT.call;
import static cli.SpecmanCLIFT.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/** Tests of the {@code --sanitize} command line operation. */
public class SanitizeTest {

  private static final String TAG_WITH_FREETEXT = "priority";
  private static final String TAG_WITHOUT_FREETEXT = "done";
  private static final String UNKNOWN_TAG = "unknown";
  private static final String INVISIBLE_TAG = "invisible";

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
