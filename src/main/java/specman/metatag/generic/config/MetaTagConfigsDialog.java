package specman.metatag.generic.config;

import specman.i18n.IDialog;

import java.awt.Window;
import java.io.Serial;

/** Window around the {@link MetaTagConfigsEditor}. The separation allows to use the editor in headless tests while the
 * dialog is only suitable to be instantiated in a real UI. */
public class MetaTagConfigsDialog extends IDialog {
  @Serial
  private static final long serialVersionUID = 1L;

  /** Shows the dialog, which closes when the user is done with the editor. */
  public MetaTagConfigsDialog(Window owner, MetaTagConfigsEditor editor) {
    super(owner, "dialog.metatag.configs.title");
    editor.onClose(this::dispose);
    getContentPane().add(editor);
    pack();
    setLocationRelativeTo(owner);
    setVisible(true);
  }
}
