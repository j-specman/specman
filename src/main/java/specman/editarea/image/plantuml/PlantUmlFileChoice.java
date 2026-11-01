package specman.editarea.image.plantuml;

import java.io.File;

/** What the user decided when asked for the source of a new PlantUML diagram: take the content of a file,
 * start with an empty diagram, or not create any diagram at all. */
public class PlantUmlFileChoice {
  private static final PlantUmlFileChoice CANCELLED = new PlantUmlFileChoice(false, null);
  private static final PlantUmlFileChoice EMPTY_DIAGRAM = new PlantUmlFileChoice(true, null);

  private final boolean emptyDiagram;
  private final File file;

  private PlantUmlFileChoice(boolean emptyDiagram, File file) {
    this.emptyDiagram = emptyDiagram;
    this.file = file;
  }

  public static PlantUmlFileChoice cancelled() { return CANCELLED; }

  public static PlantUmlFileChoice emptyDiagram() { return EMPTY_DIAGRAM; }

  public static PlantUmlFileChoice file(File file) { return new PlantUmlFileChoice(false, file); }

  public boolean isCancelled() { return !emptyDiagram && file == null; }

  public boolean isEmptyDiagram() { return emptyDiagram; }

  /** @return the file to take the source from, null if the user chose no file */
  public File file() { return file; }
}
