package specman.link.classman;

import specman.link.AliasPanel;
import specman.link.LinkLocation;

import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;

/** Asks how the link names the file when the user links a Classman diagram. The link shown is the one the model gets,
 * with the ID and the name of the diagram if it has them. There is no link without a file, as a diagram is always in
 * another file than the document. */
public class ClassmanLinkAliasPanel extends AliasPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  /** @param diagramId the ID of the diagram in the file, null if it has none
   * @param diagramName the name of the diagram in the file, null if it has none
   * @param relativeLocation the path of the file relative to the directory of the document, null if there is none, e.g.
   *                         because the document hasn't been saved yet */
  public ClassmanLinkAliasPanel(File file, String diagramId, String diagramName, LinkLocation relativeLocation,
                                Consumer<LinkLocation> locationAccepted) {
    super(file, "dialog.classman.alias.title", "dialog.classman.alias.description",
      location -> ClassmanLink.of(location, diagramId, diagramName), relativeLocation, false, locationAccepted);
  }
}
