package specman.cli;

abstract class AbstractCLO {

  /** @return the exit state, {@link SpecmanCLI#EXIT_SUCCESS} or {@link SpecmanCLI#EXIT_ERROR} */
  abstract int run() throws CLIException;

  protected static CLIException error(String message) throws CLIException {
    throw new CLIException(message);
  }

}
