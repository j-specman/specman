package specman.link;

import specman.i18n.IDialog;

import java.awt.Frame;
import java.io.Serial;

/** Window around the {@link AliasPanel}. The separation allows to use the panel in headless tests while the
 * dialog is only suitable to be instantiated in a real UI. */
public class AliasDialog extends IDialog {
  @Serial
  private static final long serialVersionUID = 1L;

  public AliasDialog(Frame owner, AliasPanel panel) {
    super(owner, "dialog.link.alias.title");
    panel.onClose(() -> setVisible(false));
    getContentPane().add(panel);
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);
    setVisible(true);
  }
}
