package classman.render;

/** Every character is 10 pixels wide, a line is 20 pixels high with its baseline at 15, bold or not. */
class FixedTextMeasurer implements TextMeasurer {

  @Override
  public double width(String text, boolean bold) {
    return text.length() * 10;
  }

  @Override
  public double lineHeight() {
    return 20;
  }

  @Override
  public double ascent() {
    return 15;
  }
}
