package specman.link.classman;

import specman.link.LinkLocation;

/** What a Classman link points to: the file, which is never the document the link is in. The ID and the name of the
 * diagram are null if the link doesn't tell them; they are informative, the file is what the link points to. */
public final class ClassmanLinkReference {
  private final LinkLocation location;
  private final String diagramId;
  private final String diagramName;

  ClassmanLinkReference(LinkLocation location, String diagramId, String diagramName) {
    this.location = location;
    this.diagramId = diagramId;
    this.diagramName = diagramName;
  }

  public LinkLocation location() { return location; }

  /** @return the alias which stands for the file, null if the link names the file by its path */
  public String alias() { return location.alias(); }

  public boolean hasAlias() { return location.isAlias(); }

  public String diagramId() { return diagramId; }

  public String diagramName() { return diagramName; }
}
