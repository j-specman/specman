package specman;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Starts another Specman process which opens a link. A Specman instance shows one model only, so a link to another
 * file needs an instance of its own. The new process is made like the running one: same Java runtime, same class path. */
public class SpecmanInstanceStarter {
  private static final String MAIN_CLASS = "specman.Specman";
  private static final String FILE_ENCODING_OPTION = "-Dfile.encoding=UTF-8";
  private static final List<String> JAVA_EXECUTABLES = List.of("javaw.exe", "java.exe", "java");

  private final File javaHome;
  private final String classPath;

  public SpecmanInstanceStarter(File javaHome, String classPath) {
    this.javaHome = javaHome;
    this.classPath = classPath;
  }

  /** @return a starter for the runtime and class path the running instance has */
  public static SpecmanInstanceStarter ofRunningInstance() {
    return new SpecmanInstanceStarter(new File(System.getProperty("java.home")), System.getProperty("java.class.path"));
  }

  /** Starts the process, which keeps running when this one ends. Its output is dropped, nobody reads it. */
  public void start(String link) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(commandLine(link));
    builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    builder.start();
  }

  /** @return the command line {@link #start(String)} runs */
  public List<String> commandLine(String link) throws IOException {
    return List.of(javaExecutable().getPath(), FILE_ENCODING_OPTION, "-cp", classPath, MAIN_CLASS, link);
  }

  /** @return the first Java executable of the runtime, preferably the one which doesn't open a console window */
  private File javaExecutable() throws IOException {
    for (String name : JAVA_EXECUTABLES) {
      File executable = new File(new File(javaHome, "bin"), name);
      if (executable.isFile()) {
        return executable;
      }
    }
    throw new IOException("No Java executable found in " + javaHome);
  }
}
