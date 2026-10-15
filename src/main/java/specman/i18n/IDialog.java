package specman.i18n;

import javax.swing.*;
import java.awt.*;

import static specman.i18n.Messages.i18n;

public class IDialog extends JDialog {

  public IDialog(Frame owner, String titleKey) {
    super(owner, i18n(titleKey), true);
  }

}
