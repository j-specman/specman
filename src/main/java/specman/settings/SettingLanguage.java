package specman.settings;

import specman.i18n.Messages;

import java.util.List;

import static specman.i18n.Messages.i18n;

public class SettingLanguage extends AbstractSelectSetting<String> {

  private static final List<SettingOption<String>> OPTIONS = List.of(
      new SettingOption<>(Messages.LANGUAGE_SYSTEM, i18n("settings.language.system")),
      new SettingOption<>("de", i18n("settings.language.de")),
      new SettingOption<>("en", i18n("settings.language.en"))
  );

  public SettingLanguage() {
    super(i18n("settings.language"), Messages.LANGUAGE_PREF, OPTIONS);
  }

  /** The language is applied at startup only, so a change needs a restart to take effect. */
  public boolean isChanged() {
    return !selectedValue().equals(Messages.savedLanguage());
  }

}
