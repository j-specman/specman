package specman.cli;

abstract class AbstractCLO {

  abstract void run();

  protected static void error(String message) {
    System.err.println("ERROR");
    System.err.println(message);
    System.exit(1);
  }

}
