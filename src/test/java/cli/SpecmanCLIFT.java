package cli;

import specman.cli.SpecmanCLI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Test access to the command line interface: calls it like {@code specman <args>} from a shell,
 * but within the test JVM, and collects what it writes to stdout and stderr.
 */
public class SpecmanCLIFT {

  private SpecmanCLIFT() {}

  /** {@code specman --sanitize <file>} */
  public static CLIResult sanitize(File file) {
    return call("--sanitize", file.getAbsolutePath());
  }

  public static CLIResult call(String... args) {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
      int exitState = SpecmanCLI.execute(args);
      return new CLIResult(exitState, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
    finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }
}
