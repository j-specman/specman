package specman.settings;

import specman.i18n.ILabel;

import javax.swing.*;

abstract class AbstractSetting {

  final JLabel label;
  final JComponent inputComponent;

  protected AbstractSetting(String labelKey, JComponent inputComponent) {
    this.label = new ILabel(labelKey, SwingConstants.TRAILING);
    this.inputComponent = inputComponent;
  }

  public abstract void savePreference();

}
