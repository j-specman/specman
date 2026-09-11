package specman.cli;

abstract class AbstractCLO {

  abstract void run() throws CLIException;

  protected static CLIException error(String message) throws CLIException {
    throw new CLIException(message);
  }

}
