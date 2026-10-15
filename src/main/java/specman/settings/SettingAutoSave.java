package specman.settings;


import java.util.List;


public class SettingAutoSave extends AbstractSelectSetting<Integer> {

  public static final String AUTOSAVE_PREF = "autosave";

  static final List<SettingOption<Integer>> OPTIONS = List.of(
      new SettingOption<>(null, "settings.autosave.off"),
      new SettingOption<>(5, "settings.autosave.5.seconds"),
      new SettingOption<>(60, "settings.autosave.1.minute"),
      new SettingOption<>(900, "settings.autosave.15.minutes")
  );

  public SettingAutoSave() {
    super("settings.autosave", AUTOSAVE_PREF, OPTIONS);
  }

  public static Integer getIntervalSeconds() {
    return loadValue(AUTOSAVE_PREF, OPTIONS, null);
  }

}
