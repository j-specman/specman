package cli;

import specman.cli.SpecmanCLI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
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

  /** {@code specman --resolve-link <link>...} */
  public static CLIResult resolveLinks(String... links) {
    String[] args = new String[links.length + 1];
    args[0] = "--resolve-link";
    System.arraycopy(links, 0, args, 1, links.length);
    return call(args);
  }

  /** {@code specman --resolve-link} with the lines the user pipes into it. */
  public static CLIResult resolveLinksFromStdin(String stdin) {
    return callWithInput(stdin, "--resolve-link");
  }

  public static CLIResult call(String... args) {
    return callWithInput("", args);
  }

  public static CLIResult callWithInput(String stdin, String... args) {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    InputStream originalIn = System.in;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
      System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
      int exitState = SpecmanCLI.execute(args);
      return new CLIResult(exitState, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
    finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
      System.setIn(originalIn);
    }
  }
}
