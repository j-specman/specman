package specman.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import specman.Specman;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Works on the real user preferences, so the original value is restored after each test
class SettingAutoLoadTest {

  private final Preferences prefs = Preferences.userNodeForPackage(Specman.class);
  private String originalValue;

  @BeforeEach
  void rememberOriginalValue() {
    originalValue = prefs.get(SettingAutoLoad.AUTOLOAD_PREF, null);
  }

  @AfterEach
  void restoreOriginalValue() {
    if (originalValue == null) {
      prefs.remove(SettingAutoLoad.AUTOLOAD_PREF);
    } else {
      prefs.put(SettingAutoLoad.AUTOLOAD_PREF, originalValue);
    }
  }

  @Test
  void legacyIntervalIsMigratedToEnabled() {
    prefs.put(SettingAutoLoad.AUTOLOAD_PREF, "5");
    SettingAutoLoad.migrateLegacyIntervalValue();
    assertEquals("true", prefs.get(SettingAutoLoad.AUTOLOAD_PREF, null));
    assertTrue(SettingAutoLoad.isEnabled());
  }

  @Test
  void legacyOffStaysDisabled() {
    prefs.put(SettingAutoLoad.AUTOLOAD_PREF, "");
    SettingAutoLoad.migrateLegacyIntervalValue();
    assertFalse(SettingAutoLoad.isEnabled());
  }

  @Test
  void booleanValuesAreKept() {
    prefs.put(SettingAutoLoad.AUTOLOAD_PREF, "true");
    SettingAutoLoad.migrateLegacyIntervalValue();
    assertEquals("true", prefs.get(SettingAutoLoad.AUTOLOAD_PREF, null));
    prefs.put(SettingAutoLoad.AUTOLOAD_PREF, "false");
    SettingAutoLoad.migrateLegacyIntervalValue();
    assertEquals("false", prefs.get(SettingAutoLoad.AUTOLOAD_PREF, null));
    assertFalse(SettingAutoLoad.isEnabled());
  }

  @Test
  void missingValueMeansDisabled() {
    prefs.remove(SettingAutoLoad.AUTOLOAD_PREF);
    SettingAutoLoad.migrateLegacyIntervalValue();
    assertNull(prefs.get(SettingAutoLoad.AUTOLOAD_PREF, null));
    assertFalse(SettingAutoLoad.isEnabled());
  }

}
