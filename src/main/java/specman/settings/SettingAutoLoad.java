package specman.settings;

import java.util.List;

public class SettingAutoLoad extends AbstractSelectSetting<Integer> {

  public static final String AUTOLOAD_PREF = "autoload";

  static final List<SettingOption<Integer>> OPTIONS = SettingAutoSave.OPTIONS;

  public SettingAutoLoad() {
    super("Automatisches Laden", AUTOLOAD_PREF, OPTIONS);
  }

  public static Integer getIntervalSeconds() {
    return loadValue(AUTOLOAD_PREF, OPTIONS, null);
  }

}
