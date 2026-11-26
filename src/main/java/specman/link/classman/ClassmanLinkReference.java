package specman.link.classman;

import java.io.File;

/** What a Classman link points to. Exactly one of {@code file} and {@code alias} is set. The ID and the name of the
 * diagram are null if the link doesn't tell them; they are informative, the file is what the link points to. */
public final class ClassmanLinkReference {
  private final File file;
  private final String alias;
  private final String diagramId;
  private final String diagramName;

  ClassmanLinkReference(File file, String alias, String diagramId, String diagramName) {
    this.file = file;
    this.alias = alias;
    this.diagramId = diagramId;
    this.diagramName = diagramName;
  }

  /** @return the file if the link names it by its path, null if it uses an alias */
  public File file() { return file; }

  /** @return the alias which stands for the file, null if the link names the path */
  public String alias() { return alias; }

  public boolean hasAlias() { return alias != null; }

  public String diagramId() { return diagramId; }

  public String diagramName() { return diagramName; }
}
