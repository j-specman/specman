package specman.cli;

public class SpecmanCLI {

  public static final int EXIT_SUCCESS = 0;
  public static final int EXIT_ERROR = 1;

  private static final String OPTION_PREFIX = "--";

  /** @return true if the first argument is an option like {@code --sanitize}, which makes the call a CLI operation
   * instead of starting the GUI. Files and links to open have no such prefix. */
  public static boolean isCLIInvocation(String[] args) {
    return args.length > 0 && args[0].startsWith(OPTION_PREFIX);
  }

  public static void run(String[] args) {
    forceImmediateExit(execute(args));
  }

  /** Executes the CLI operation specified by the arguments and returns the exit state
   * ({@link #EXIT_SUCCESS} or {@link #EXIT_ERROR}) instead of terminating the JVM. {@link #run(String[])}
   * is the production entry point, this method is the seam for headless tests. */
  public static int execute(String[] args) {
    try {
      System.setProperty("java.awt.headless", "true");

      CLIOperation operation = CLIOperation.fromArgs(args);
      if (operation == CLIOperation.SANITIZE) {
        return new SanitizeCLO(args).execute();
      }
      if (operation == CLIOperation.RESOLVE_LINK) {
        return new ResolveLinkCLO(args).execute();
      }
      if (operation == CLIOperation.VERSION) {
        return new VersionCLO(args).execute();
      }
      throw new CLIException(unknownOptionMessage(args));
    }
    catch(CLIException clix) {
      System.err.println("ERROR");
      System.err.println(clix.getMessage());
      return EXIT_ERROR;
    }
  }

  private static String unknownOptionMessage(String[] args) {
    String option = args.length > 0 ? args[0] : "";
    return "Unknown option: " + option + ". Valid options: " + CLIOperation.validOptions();
  }

  /** Force JVM shutdown: The CLI operation might have called SwingUtilities.invokeAndWait()
   * internally which starts a non-daemon AWT event dispatcher thread in the background.
   * Without System.exit() the EDT may keep the JVM alive for a second after the operation
   * has finished its job, and this is of no use in CLI mode. */
  private static void forceImmediateExit(int exitState) {
    System.exit(exitState);
  }
}
