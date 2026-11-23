package specman.cli;

import specman.SpecmanVersion;

import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/**
 * Prints the version of Specman, which allows other tools to find out which Specman they are talking to without
 * deriving it from the name of the jar file.
 * <p>
 * The output is the bare version number in one line, e.g. {@code 1.4.1}. If the version is not known, the operation fails
 * rather than printing a version nobody can interpret.
 * <p>
 * Exit code 0 = version printed, 1 = ERROR.
 */
public class VersionCLO extends AbstractCLO {

  VersionCLO(String[] args) {
  }

  @Override
  int run() throws CLIException {
    String version = SpecmanVersion.getVersion();
    assertVersionKnown(version);
    out.println(version);
    return EXIT_SUCCESS;
  }

  private static void assertVersionKnown(String version) throws CLIException {
    if (SpecmanVersion.UNKNOWN_VERSION.equals(version)) {
      throw error("The version of Specman is not known");
    }
  }
}
