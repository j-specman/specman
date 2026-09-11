package specman.cli;

public class SpecmanCLI {

  public static void run(String[] args) {
    try {
      System.setProperty("java.awt.headless", "true");

      CLIOperation operation = CLIOperation.fromArgs(args);
      if (operation == CLIOperation.SANITIZE) {
        new SanitizeCLO(args).run();
      }
      forceImmediateExit(0);
    }
    catch(CLIException clix) {
      System.err.println("ERROR");
      System.err.println(clix.getMessage());
      forceImmediateExit(1);
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
