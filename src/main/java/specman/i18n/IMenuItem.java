package specman.i18n;

import javax.swing.*;

import static specman.i18n.Messages.i18n;

public class IMenuItem extends JMenuItem {

  public IMenuItem(String messageKey) {
    super(i18n(messageKey));
  }

  public IMenuItem(String messageKey, Icon icon) {
    super(i18n(messageKey), icon);
  }

}
