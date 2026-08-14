package specman.settings;

class SettingOption<T> {

  final T value;
  final String label;

  SettingOption(T value, String label) {
    this.value = value;
    this.label = label;
  }

  @Override
  public String toString() { return label; }

}
