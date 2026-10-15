package specman.i18n;

import javax.swing.*;

import static specman.i18n.Messages.i18n;

public class IRadioButton extends JRadioButton {

  public IRadioButton(String messageKey) {
    super(i18n(messageKey));
  }

  public IRadioButton(String messageKey, boolean selected) {
    super(i18n(messageKey), selected);
  }

}
