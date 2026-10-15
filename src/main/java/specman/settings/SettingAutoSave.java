package specman.settings;


import java.util.List;

import static specman.i18n.Messages.i18n;

public class SettingAutoSave extends AbstractSelectSetting<Integer> {

  public static final String AUTOSAVE_PREF = "autosave";

  static final List<SettingOption<Integer>> OPTIONS = List.of(
      new SettingOption<>(null, i18n("settings.autosave.off")),
      new SettingOption<>(5, i18n("settings.autosave.5.seconds")),
      new SettingOption<>(60, i18n("settings.autosave.1.minute")),
      new SettingOption<>(900, i18n("settings.autosave.15.minutes"))
  );

  public SettingAutoSave() {
    super(i18n("settings.autosave"), AUTOSAVE_PREF, OPTIONS);
  }

  public static Integer getIntervalSeconds() {
    return loadValue(AUTOSAVE_PREF, OPTIONS, null);
  }

}
