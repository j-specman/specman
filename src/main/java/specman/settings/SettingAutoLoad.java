package specman.settings;

import specman.Specman;

import java.util.prefs.Preferences;


public class SettingAutoLoad extends AbstractBooleanSetting {

  public static final String AUTOLOAD_PREF = "autoload";

  static {
    migrateLegacyIntervalValue();
  }

  public SettingAutoLoad() {
    super("settings.autoload", AUTOLOAD_PREF, false);
  }

  public static boolean isEnabled() {
    return isSet(AUTOLOAD_PREF, false);
  }

  public static void setEnabled(boolean enabled) {
    Preferences.userNodeForPackage(Specman.class).put(AUTOLOAD_PREF, Boolean.toString(enabled));
  }

  /** Up to version 1.3.1, auto load was polling and the preference held the interval in seconds. */
  static void migrateLegacyIntervalValue() {
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);
    if (prefs.get(AUTOLOAD_PREF, "").matches("\\d+")) {
      prefs.putBoolean(AUTOLOAD_PREF, true);
    }
  }

}
