package specman.settings;

import javax.swing.*;

abstract class AbstractSetting {

  final JLabel label;
  final JComponent inputComponent;

  protected AbstractSetting(String labelText, JComponent inputComponent) {
    this.label = new JLabel(labelText, SwingConstants.TRAILING);
    this.inputComponent = inputComponent;
  }

  public abstract void savePreference();

}
