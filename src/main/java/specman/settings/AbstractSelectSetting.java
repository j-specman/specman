package specman.settings;

import specman.Specman;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

abstract class AbstractSelectSetting<T> extends AbstractSetting {

  private final JComboBox<SettingOption<T>> comboBox;
  private final String prefKey;

  public AbstractSelectSetting(String label, String prefKey, List<SettingOption<T>> options) {
    this(label, prefKey, options, new JComboBox<>());
  }

  // Two-step constructor: comboBox is created before super() runs so the field
  // assignment and preference loading below can safely reference it.
  private AbstractSelectSetting(String label, String prefKey, List<SettingOption<T>> options, JComboBox<SettingOption<T>> comboBox) {
    super(label, comboBox);
    this.comboBox = comboBox;
    this.prefKey = prefKey;
    options.forEach(comboBox::addItem);
    String saved = Preferences.userNodeForPackage(Specman.class).get(prefKey, "");
    options.stream()
        .filter(o -> serialise(o.value).equals(saved))
        .findFirst()
        .ifPresent(comboBox::setSelectedItem);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void savePreference() {
    SettingOption<T> selected = (SettingOption<T>) comboBox.getSelectedItem();
    Preferences.userNodeForPackage(Specman.class).put(prefKey, selected != null ? serialise(selected.value) : "");
  }

  protected static <T> T loadValue(String prefKey, List<SettingOption<T>> options, T defaultValue) {
    String saved = Preferences.userNodeForPackage(Specman.class).get(prefKey, serialise(defaultValue));
    Optional<SettingOption<T>> found = options.stream()
        .filter(o -> serialise(o.value).equals(saved))
        .findFirst();
    return found.isPresent() ? found.get().value : defaultValue;
  }

  private static String serialise(Object value) {
    return value == null ? "" : value.toString();
  }

}
