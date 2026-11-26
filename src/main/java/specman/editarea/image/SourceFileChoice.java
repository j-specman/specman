package specman.editarea.image;

import java.io.File;

/** What the user decided when asked for the source of a new diagram shown from its source: take the content of a file,
 * link the file, start with an empty diagram, or not create any diagram at all. */
public class SourceFileChoice {
  private static final SourceFileChoice CANCELLED = new SourceFileChoice(false, null, false);
  private static final SourceFileChoice EMPTY_DIAGRAM = new SourceFileChoice(true, null, false);

  private final boolean emptyDiagram;
  private final File file;
  private final boolean linked;

  private SourceFileChoice(boolean emptyDiagram, File file, boolean linked) {
    this.emptyDiagram = emptyDiagram;
    this.file = file;
    this.linked = linked;
  }

  public static SourceFileChoice cancelled() { return CANCELLED; }

  public static SourceFileChoice emptyDiagram() { return EMPTY_DIAGRAM; }

  /** Take the content of the file. */
  public static SourceFileChoice file(File file) { return new SourceFileChoice(false, file, false); }

  /** Keep the content in the file, and link to it. */
  public static SourceFileChoice linkedFile(File file) { return new SourceFileChoice(false, file, true); }

  public boolean isCancelled() { return !emptyDiagram && file == null; }

  public boolean isEmptyDiagram() { return emptyDiagram; }

  /** @return true if the diagram is to link to the file instead of taking its content */
  public boolean isLinked() { return linked; }

  /** @return the file to take the source from, or to link, null if the user chose no file */
  public File file() { return file; }
}
