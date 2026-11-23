package cli;

import org.junit.jupiter.api.Test;
import specman.SpecmanVersion;

import static cli.SpecmanCLIFT.call;
import static cli.SpecmanCLIFT.version;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.cli.SpecmanCLI.EXIT_ERROR;
import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/** Tests of the {@code --version} command line operation and the handling of unknown options. */
public class VersionTest {

  private static final String UNKNOWN_OPTION = "--nosuchoption";

  /** The version is printed as bare number in one line, and nothing is written to stderr. */
  @Test
  void testVersionIsPrintedAsBareNumber() {
    CLIResult result = version();

    assertEquals(EXIT_SUCCESS, result.exitState, result.toString());
    assertEquals(SpecmanVersion.getVersion() + System.lineSeparator(), result.stdout);
    assertEquals("", result.stderr);
  }

  /** An unknown option is reported as ERROR naming the option and the valid ones, instead of starting the GUI. */
  @Test
  void testUnknownOptionIsError() {
    CLIResult result = call(UNKNOWN_OPTION);

    assertEquals(EXIT_ERROR, result.exitState, result.toString());
    assertTrue(result.stderr.startsWith("ERROR"), result.toString());
    assertTrue(result.stderr.contains("Unknown option: " + UNKNOWN_OPTION), result.toString());
    assertTrue(result.stderr.contains("--sanitize") && result.stderr.contains("--version"), result.toString());
    assertEquals("", result.stdout);
  }
}
