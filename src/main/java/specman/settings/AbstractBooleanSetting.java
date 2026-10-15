package specman.settings;

import specman.Specman;

import javax.swing.*;
import java.util.prefs.Preferences;

abstract class AbstractBooleanSetting extends AbstractSetting {

  private final JCheckBox checkBox;
  private final String prefKey;

  public AbstractBooleanSetting(String labelKey, String prefKey, boolean defaultValue) {
    this(labelKey, prefKey, defaultValue, new JCheckBox());
  }

  // Two-step constructor: checkBox is created before super() runs, so the field
  // assignment and loadPreference() below can safely reference it.
  private AbstractBooleanSetting(String labelKey, String prefKey, boolean defaultValue, JCheckBox checkBox) {
    super(labelKey, checkBox);
    this.checkBox = checkBox;
    this.prefKey = prefKey;
    checkBox.setSelected(Preferences.userNodeForPackage(Specman.class).getBoolean(prefKey, defaultValue));
  }

  protected static boolean isSet(String prefKey, boolean defaultValue) {
    return Preferences.userNodeForPackage(Specman.class).getBoolean(prefKey, defaultValue);
  }

  @Override
  public void savePreference() {
    Preferences.userNodeForPackage(Specman.class).put(prefKey, Boolean.toString(checkBox.isSelected()));
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

}
