package specman.settings;

public class SettingAutoSave extends AbstractBooleanSetting {
  public static final String AUTOSAVE_PREF = "autosave";

  public SettingAutoSave() {
    super("Automatisches Speichern", AUTOSAVE_PREF, false);
  }

  public static boolean isSet() {
    return AbstractBooleanSetting.isSet(AUTOSAVE_PREF, false);
  }

}
