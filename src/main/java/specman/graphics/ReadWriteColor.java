package specman.graphics;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.util.Locale;

/**
 * Bundles the two representations of a color that Swing's {@link javax.swing.text.html.HTMLEditorKit}
 * requires for the asymmetric read/write access to background colors in an HTML document.
 * <p>
 * <b>Write path:</b> Colors are applied via {@link javax.swing.text.StyleConstants} on a
 * {@link javax.swing.text.MutableAttributeSet}. This is the typed Swing API and works with
 * Java {@link java.awt.Color} objects.
 * <p>
 * <b>Read path:</b> When the editor kit parses HTML, it stores CSS attributes internally as
 * strings (e.g. {@code "#ffff00"}). Reading back a background color via
 * {@link javax.swing.text.html.CSS.Attribute#BACKGROUND_COLOR} therefore returns a CSS string,
 * not a {@link java.awt.Color}. Detecting whether a color is currently set requires comparing
 * against that same string representation.
 */
public class ReadWriteColor {
  public final Color color;
  public final String htmlColor;
  public final MutableAttributeSet background;

  public ReadWriteColor(Color color) {
    this.color = color;
    this.htmlColor = toHTMLColor(color);
    this.background = new SimpleAttributeSet();
    StyleConstants.setBackground(background, color);
  }

  public boolean isBackground(String cssColor) {
    return cssColor != null && htmlColor.equalsIgnoreCase(cssColor);
  }

  public static String toHTMLColor(Color color) {
    if (color == null) {
      return "#000000";
    }
    else if (color.getAlpha() != 255) {
      return String.format(Locale.US, "rgba(%d, %d, %d, %1.1f)",
        color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255f);
    }
    return "#" + Integer.toHexString(color.getRGB()).substring(2).toLowerCase();
  }
}
