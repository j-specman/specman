package specman.settings;

import java.util.List;

public class SettingAutoSave extends AbstractSelectSetting<Integer> {

  public static final String AUTOSAVE_PREF = "autosave";

  static final List<SettingOption<Integer>> OPTIONS = List.of(
      new SettingOption<>(null, "Off"),
      new SettingOption<>(5, "5 Seconds"),
      new SettingOption<>(60, "1 Minute"),
      new SettingOption<>(900, "15 Minutes")
  );

  public SettingAutoSave() {
    super("Automatisches Speichern", AUTOSAVE_PREF, OPTIONS);
  }

  public static Integer getIntervalSeconds() {
    return loadValue(AUTOSAVE_PREF, OPTIONS, null);
  }

}
