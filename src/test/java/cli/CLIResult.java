package cli;

/** What a CLI call wrote to stdout and stderr, and the exit state it ended with. */
public class CLIResult {
  public final int exitState;
  public final String stdout;
  public final String stderr;

  CLIResult(int exitState, String stdout, String stderr) {
    this.exitState = exitState;
    this.stdout = stdout;
    this.stderr = stderr;
  }

  @Override
  public String toString() {
    return "exit state: " + exitState + "\nstdout: '" + stdout + "'\nstderr: '" + stderr + "'";
  }
}
