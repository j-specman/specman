package specman.link.classman;

import specman.link.AliasPanel;

import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;

/** Asks for the alias of the file when the user links a Classman diagram. The link shown is the one the model gets,
 * with the ID and the name of the diagram if it has them. */
public class ClassmanLinkAliasPanel extends AliasPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  /** @param diagramId the ID of the diagram in the file, null if it has none
   * @param diagramName the name of the diagram in the file, null if it has none */
  public ClassmanLinkAliasPanel(File file, String diagramId, String diagramName, Consumer<String> aliasAccepted) {
    super(file, "dialog.classman.alias.title", "dialog.classman.alias.description",
      alias -> ClassmanLink.of(file, alias, diagramId, diagramName), aliasAccepted);
  }
}
