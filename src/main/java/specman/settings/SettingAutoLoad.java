package specman.settings;

import specman.Specman;

import java.util.prefs.Preferences;

import static specman.i18n.Messages.i18n;

public class SettingAutoLoad extends AbstractBooleanSetting {

  public static final String AUTOLOAD_PREF = "autoload";

  static {
    migrateLegacyIntervalValue();
  }

  public SettingAutoLoad() {
    super(i18n("settings.autoload"), AUTOLOAD_PREF, false);
  }

  public static boolean isEnabled() {
    return isSet(AUTOLOAD_PREF, false);
  }

  /** Up to version 1.3.1, auto load was polling and the preference held the interval in seconds. */
  static void migrateLegacyIntervalValue() {
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);
    if (prefs.get(AUTOLOAD_PREF, "").matches("\\d+")) {
      prefs.putBoolean(AUTOLOAD_PREF, true);
    }
  }

}
