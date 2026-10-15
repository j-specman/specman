package specman.i18n;

import javax.swing.*;

import static specman.i18n.Messages.i18n;

public class ILabel extends JLabel {

  public ILabel(String messageKey) {
    super(i18n(messageKey));
  }

  public ILabel(String messageKey, int horizontalAlignment) {
    super(i18n(messageKey), horizontalAlignment);
  }

}
