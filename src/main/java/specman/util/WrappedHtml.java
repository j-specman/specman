package specman.util;

/** Swing labels and tooltips only wrap text which is HTML with a defined width. */
public final class WrappedHtml {

  private WrappedHtml() {}

  /** The plain text as HTML which wraps at the given width in pixels. Characters with a meaning in HTML are
   * escaped and line breaks are kept. */
  public static String of(String text, int widthPixels) {
    String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    return "<html><body style='width: " + widthPixels + "px'>" + escaped + "</body></html>";
  }
}
