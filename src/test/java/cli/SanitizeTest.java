package cli;

import org.junit.jupiter.api.Test;

import java.io.File;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static cli.SpecmanCLIFT.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the {@code --sanitize} command line operation. */
public class SanitizeTest {

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

  private void assertERROR(CLIResult result, String... fragments) {
    assertEquals(1, result.exitState, result.toString());
    assertContains(result.stderr, "ERROR");
    for (String fragment : fragments) {
      assertContains(result.stderr, fragment);
    }
  }

  private void assertContains(String content, String fragment) {
    assertTrue(content.contains(fragment), "Expected fragment '" + fragment + "' not found in '" + content + "'");
  }

  private void assertOK(CLIResult result) {
    assertEquals(0, result.exitState, result.toString());
    assertContains(result.stdout, "OK");
  }

}
