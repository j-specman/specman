package specman.link;

import specman.i18n.IDialog;

import java.awt.Window;
import java.io.Serial;

/** Window around the {@link AliasListPanel}. The separation allows to use the panel in headless tests while the
 * dialog is only suitable to be instantiated in a real UI. */
public class AliasListDialog extends IDialog {
  @Serial
  private static final long serialVersionUID = 1L;

  public AliasListDialog(Window owner, AliasListPanel panel) {
    super(owner, "dialog.link.aliases.title");
    panel.onClose(() -> setVisible(false));
    getContentPane().add(panel);
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);
    setVisible(true);
  }
}
