package specman.editarea.image;

import java.io.File;

/** What the user decided when asked for the source of a new diagram shown from its source: take the content of a file,
 * start with an empty diagram, or not create any diagram at all. */
public class SourceFileChoice {
  private static final SourceFileChoice CANCELLED = new SourceFileChoice(false, null);
  private static final SourceFileChoice EMPTY_DIAGRAM = new SourceFileChoice(true, null);

  private final boolean emptyDiagram;
  private final File file;

  private SourceFileChoice(boolean emptyDiagram, File file) {
    this.emptyDiagram = emptyDiagram;
    this.file = file;
  }

  public static SourceFileChoice cancelled() { return CANCELLED; }

  public static SourceFileChoice emptyDiagram() { return EMPTY_DIAGRAM; }

  public static SourceFileChoice file(File file) { return new SourceFileChoice(false, file); }

  public boolean isCancelled() { return !emptyDiagram && file == null; }

  public boolean isEmptyDiagram() { return emptyDiagram; }

  /** @return the file to take the source from, null if the user chose no file */
  public File file() { return file; }
}
