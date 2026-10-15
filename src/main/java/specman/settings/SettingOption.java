package specman.settings;

import static specman.i18n.Messages.i18n;

class SettingOption<T> {

  final T value;
  final String label;

  SettingOption(T value, String labelKey) {
    this.value = value;
    this.label = i18n(labelKey);
  }

  @Override
  public String toString() { return label; }

}
