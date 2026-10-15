package specman.i18n;

import javax.swing.*;

import static specman.i18n.Messages.i18n;

public class ICheckBox extends JCheckBox {

  public ICheckBox(String messageKey) {
    super(i18n(messageKey));
  }

  public ICheckBox(String messageKey, boolean selected) {
    super(i18n(messageKey), selected);
  }

}
