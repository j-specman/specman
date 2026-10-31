package specman.cli;

abstract class AbstractCLO {

  static final int EXIT_SUCCESS = 0;
  static final int EXIT_ERROR = 1;

  /** @return the exit state, {@link #EXIT_SUCCESS} or {@link #EXIT_ERROR} */
  abstract int run() throws CLIException;

  protected static CLIException error(String message) throws CLIException {
    throw new CLIException(message);
  }

}
