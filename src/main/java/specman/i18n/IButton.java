package specman.i18n;

import javax.swing.*;

import static specman.i18n.Messages.i18n;

public class IButton extends JButton {

  public IButton(String messageKey) {
    super(i18n(messageKey));
  }

}
