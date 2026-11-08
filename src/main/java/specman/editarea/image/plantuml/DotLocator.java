package specman.editarea.image.plantuml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Finds a Graphviz {@code dot} which the user has made available on purpose. PlantUML's own search is not
 * good enough for that: on Windows it falls back to a minimal dot it carries along, even if a full Graphviz
 * is installed, and on Linux and macOS it only looks at a few fixed places. Specman only wants to switch to
 * Graphviz for a dot the user can see in the environment, the minimal one is never used.
 * <p>
 * The places are tried in this order, the first dot which works wins:
 * <ol>
 *   <li>the environment variable GRAPHVIZ_DOT (the one PlantUML uses as well)</li>
 *   <li>the directories of the PATH</li>
 * </ol>
 * There is deliberately no search through the places Graphviz is usually installed to: it would make
 * Specman switch the layout engine without anybody having asked for it.
 */
public class DotLocator {
  public static final String ENVIRONMENT_VARIABLE = "GRAPHVIZ_DOT";

  private static final String PATH_VARIABLE = "PATH";
  private static final String WINDOWS_EXECUTABLE = "dot.exe";
  private static final String UNIX_EXECUTABLE = "dot";
  private static final int VERSION_TIMEOUT_SECONDS = 5;

  private final Map<String, String> environment;
  private final String executableName;
  private final Predicate<File> isWorkingDot;

  /** @param environment the environment variables, as System.getenv() has them
   * @param executableName the file name of dot on this platform
   * @param isWorkingDot tells whether the file is a dot which can be run */
  public DotLocator(Map<String, String> environment, String executableName, Predicate<File> isWorkingDot) {
    this.environment = environment;
    this.executableName = executableName;
    this.isWorkingDot = isWorkingDot;
  }

  public static DotLocator forThisMachine() {
    boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    return new DotLocator(System.getenv(), windows ? WINDOWS_EXECUTABLE : UNIX_EXECUTABLE, DotLocator::runsAsDot);
  }

  /** Asks the file for its version: only a dot which can be run answers with its name and exit code 0. */
  private static boolean runsAsDot(File dot) {
    ProcessBuilder processBuilder = new ProcessBuilder(List.of(dot.getAbsolutePath(), "-V"));
    processBuilder.redirectErrorStream(true);
    try {
      Process process = processBuilder.start();
      process.getOutputStream().close();
      if (!process.waitFor(VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return false;
      }
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      return process.exitValue() == 0 && output.toLowerCase(Locale.ROOT).contains("graphviz");
    }
    catch (IOException e) {
      return false;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** @return the first dot which works, null if there is none */
  public File locate() {
    for (File candidate : candidates()) {
      if (candidate.isFile() && isWorkingDot.test(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private List<File> candidates() {
    List<File> candidates = new ArrayList<>();
    String fromVariable = environment.get(ENVIRONMENT_VARIABLE);
    if (fromVariable != null && !fromVariable.isBlank()) {
      candidates.add(new File(fromVariable.strip()));
    }
    for (File directory : pathDirectories()) {
      candidates.add(new File(directory, executableName));
    }
    return candidates;
  }

  private List<File> pathDirectories() {
    List<File> directories = new ArrayList<>();
    String path = environment.get(PATH_VARIABLE);
    if (path == null) {
      return directories;
    }
    for (String directory : path.split(File.pathSeparator)) {
      if (!directory.isBlank()) {
        directories.add(new File(directory.strip()));
      }
    }
    return directories;
  }
}
