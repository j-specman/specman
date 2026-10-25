package specman.cli;

public class SpecmanCLI {

  public static void run(String[] args) {
    forceImmediateExit(execute(args));
  }

  /** Executes the CLI operation specified by the arguments and returns the exit state
   * (0 = success, 1 = error) instead of terminating the JVM. {@link #run(String[])} is the
   * production entry point, this method is the seam for headless tests. */
  public static int execute(String[] args) {
    try {
      System.setProperty("java.awt.headless", "true");

      CLIOperation operation = CLIOperation.fromArgs(args);
      if (operation == CLIOperation.SANITIZE) {
        new SanitizeCLO(args).run();
      }
      return 0;
    }
    catch(CLIException clix) {
      System.err.println("ERROR");
      System.err.println(clix.getMessage());
      return 1;
    }
  }

  /** Force JVM shutdown: The CLI operation might have called SwingUtilities.invokeAndWait()
   * internally which starts a non-daemon AWT event dispatcher thread in the background.
   * Without System.exit() the EDT may keep the JVM alive for a second after the operation
   * has finished its job, and this is of no use in CLI mode. */
  private static void forceImmediateExit(int exitState) {
    System.exit(exitState);
  }
}
