package specman.cli;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class AbstractCLO {

  private static final String ERROR_LINE_INDENT = "\n  ";

  /** The standard output of the operation, UTF-8 encoded on all platforms. It is flushed by {@link #execute()}. */
  protected final PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);

  /** Runs the operation and makes sure that everything it has written to {@link #out} is delivered.
   * @return the exit state, {@link SpecmanCLI#EXIT_SUCCESS} or {@link SpecmanCLI#EXIT_ERROR} */
  final int execute() throws CLIException {
    try {
      return run();
    }
    finally {
      out.flush();
    }
  }

  /** @return the exit state, {@link SpecmanCLI#EXIT_SUCCESS} or {@link SpecmanCLI#EXIT_ERROR} */
  abstract int run() throws CLIException;

  protected static CLIException error(String message) throws CLIException {
    throw new CLIException(message);
  }

  protected static void assertThat(boolean condition, String message) throws CLIException {
    if (!condition) {
      throw error(message);
    }
  }

  protected static void assertFileExists(File file) throws CLIException {
    assertThat(file.exists(), "File not found: " + file.getAbsolutePath());
  }

  /** Fails with the heading followed by one line for each error, if there is any. */
  public static void assertNoErrors(String heading, List<String> errors) throws CLIException {
    assertThat(errors.isEmpty(), heading + ":" + ERROR_LINE_INDENT + String.join(ERROR_LINE_INDENT, errors));
  }

}
