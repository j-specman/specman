package classman.render;

/** Every character is 10 pixels wide, a line is 20 pixels high with its baseline at 15, bold or not. For another font
 * size than the default one, these measures grow and shrink with the size. */
class FixedTextMeasurer implements TextMeasurer {
  private final double scale;

  FixedTextMeasurer() {
    this(TextFont.DEFAULT);
  }

  private FixedTextMeasurer(TextFont font) {
    scale = (double) font.size / TextFont.DEFAULT.size;
  }

  @Override
  public double width(String text, boolean bold) {
    return text.length() * 10 * scale;
  }

  @Override
  public double lineHeight() {
    return 20 * scale;
  }

  @Override
  public double ascent() {
    return 15 * scale;
  }

  @Override
  public TextMeasurer forFont(TextFont font) {
    return new FixedTextMeasurer(font);
  }
}
