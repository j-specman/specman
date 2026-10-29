package specman.i18n;

import javax.swing.*;
import java.awt.*;

import static specman.i18n.Messages.i18n;

public class IDialog extends JDialog {

  /** @param owner the frame or dialog the new dialog is modal to, may be null */
  public IDialog(Window owner, String titleKey) {
    super(owner, i18n(titleKey), ModalityType.APPLICATION_MODAL);
  }

}
