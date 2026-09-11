package specman.cli;

public class SpecmanCLI {

  public static void run(String[] args) {
    System.setProperty("java.awt.headless", "true");

    CLIOperation operation = CLIOperation.fromArgs(args);
    if (operation == CLIOperation.SANITIZE) {
      new SanitizeCLO(args).run();
    }
  }
}
