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
import static cli.SpecmanCLIFT.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/** Tests of the {@code --sanitize} command line operation. */
public class SanitizeTest {

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
