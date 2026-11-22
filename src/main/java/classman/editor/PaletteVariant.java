package classman.editor;

/** One of the things which a palette button can create: an element kind or a relation type.
 * @param <T> what the variant stands for */
class PaletteVariant<T> {
  final T value;
  final String iconPath;
  private final String key;

  /** @param key tells the texts of the variant: palette.label.key and palette.create.key */
  PaletteVariant(String key, T value, String iconPath) {
    this.key = key;
    this.value = value;
    this.iconPath = iconPath;
  }

  String key() {
    return key;
  }
}
