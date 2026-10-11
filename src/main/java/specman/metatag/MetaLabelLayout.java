package specman.metatag;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * The layout of the content of a meta label: the labels in the meta strip at the top of a step,
 * i.e. the step number and the meta tags, are {@link JLabel}s with icon and text, and this class
 * decides where icon and text sit within them. That is: where Swing puts them, and how far the
 * text has to be lifted to look vertically centered. All labels of the strip use this rule, so
 * their baselines are the same.
 * <p>
 * A label of the strip is only as high as the step number label minus a trim, which leaves hardly
 * any room for the text. Swing centers the font's line box, whose ascent is larger than its
 * descent, so depending on the zoom factor the text sits too low and the descenders of "g" or "y"
 * touch the lower edge. The text is lifted via the bottom inset of the label's border just as far
 * as needed to leave a pixel of air below the descenders, but never so far that the capital letters
 * have less than a pixel of air above. This is a matter of single pixel rows, so it is based on
 * what is actually drawn.
 * <p>
 * "Ink" in the names of this class is the typographer's word for the part of a text which is
 * actually painted, as opposed to the space the font reserves for it (the line box with its ascent
 * and descent). Java calls the same thing "visual bounds", e.g.
 * {@link java.awt.font.GlyphVector#getVisualBounds()}, and the reserved space "logical bounds".
 * Pango and Cairo speak of "ink extents".
 * <p>
 * The label has to apply {@link #topInset()} and {@link #bottomInset()} as the insets of its border
 * after {@link #align(int, int)} reported a change, and to call that whenever it got its bounds.
 */
public final class MetaLabelLayout {
  // A pixel row counts as covered by a glyph if it is covered at least half
  private static final int INK_MIN_ALPHA = 128;
  // Shared by all labels: the ink rows depend on nothing but the font, the size of which changes
  // with the zoom factor. So after a zoom change each font is measured once, no matter how many
  // labels there are.
  private static final Map<Font, InkRows> INK_ROWS_BY_FONT = new ConcurrentHashMap<>();

  private final JLabel label;
  private int topInset = 0;
  private int bottomInset = 0;
  private int iconLift = 0;
  // The inputs the insets were worked out for, to spare the work if none of them changed
  private int alignedHeight;
  private int alignedOverdrawnTopRows;
  private Font alignedFont;
  private boolean alignedTextEmpty;
  private int alignedIconHeight;

  public MetaLabelLayout(JLabel label) {
    this.label = label;
  }

  /** The layout computation the label itself uses to place icon and text. */
  public void layout(Rectangle viewRect, Rectangle iconRect, Rectangle textRect) {
    SwingUtilities.layoutCompoundLabel(label, label.getFontMetrics(label.getFont()), label.getText(), label.getIcon(),
      label.getVerticalAlignment(), label.getHorizontalAlignment(),
      label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
      viewRect, iconRect, textRect, label.getIconTextGap());
  }

  /** Works out the insets for a label of the given height. A label without text has nothing to lift.
   * @param overdrawnTopRows the number of rows at the top of the label which something else paints
   *   over, so the text is centered in what remains visible, and kept out of the overdrawn rows
   * @return whether the insets for the label's border changed */
  public boolean align(int height, int overdrawnTopRows) {
    if (height <= 0) {
      return false;
    }
    boolean textEmpty = label.getText().isEmpty();
    int iconHeight = label.getIcon() == null ? -1 : label.getIcon().getIconHeight();
    if (height == alignedHeight && overdrawnTopRows == alignedOverdrawnTopRows
        && label.getFont().equals(alignedFont) && textEmpty == alignedTextEmpty && iconHeight == alignedIconHeight) {
      return false;
    }
    alignedHeight = height;
    alignedOverdrawnTopRows = overdrawnTopRows;
    alignedFont = label.getFont();
    alignedTextEmpty = textEmpty;
    alignedIconHeight = iconHeight;

    int visibleHeight = height - overdrawnTopRows;
    int newBottomInset = textEmpty ? 0 : findBottomInset(visibleHeight);
    iconLift = label.getIcon() == null ? 0 : iconY(visibleHeight, 0) - iconY(visibleHeight, newBottomInset);
    boolean changed = newBottomInset != bottomInset || overdrawnTopRows != topInset;
    bottomInset = newBottomInset;
    topInset = overdrawnTopRows;
    return changed;
  }

  public int topInset() {
    return topInset;
  }

  public int bottomInset() {
    return bottomInset;
  }

  /** @return the number of pixels the icon would move up with the bottom inset, and has to be
   * painted lower to stay where it is without the inset */
  public int iconLift() {
    return iconLift;
  }

  private int findBottomInset(int visibleHeight) {
    InkRows inkRows = INK_ROWS_BY_FONT.computeIfAbsent(label.getFont(), MetaLabelLayout::measureInkRows);
    FontMetrics metrics = label.getFontMetrics(label.getFont());
    int inset = 0;
    while (inset < visibleHeight
        && baselineY(visibleHeight, inset, metrics) + inkRows.descenderRows >= visibleHeight
        && baselineY(visibleHeight, inset + 1, metrics) - inkRows.capitalRows >= 1) {
      inset++;
    }
    return inset;
  }

  private int baselineY(int height, int inset, FontMetrics metrics) {
    Rectangle textRect = new Rectangle();
    layout(new Rectangle(0, 0, label.getWidth(), height - inset), new Rectangle(), textRect);
    return textRect.y + metrics.getAscent();
  }

  private int iconY(int height, int inset) {
    Rectangle iconRect = new Rectangle();
    layout(new Rectangle(0, 0, label.getWidth(), height - inset), iconRect, new Rectangle());
    return iconRect.y;
  }

  /** Measures how many pixel rows the tallest letters reach above and the deepest letters reach
   * below the baseline in the font. Measured on what is actually drawn, not on the outlines of
   * the glyphs: hinting and rounding let those differ by a row, which is a lot at this size.
   * <p>
   * The tallest letters are the capitals and the ascenders (b, d, f, h, k, l), which are usually a
   * row taller than the capitals, the deepest ones are the descenders (g, j, p, q, y). Accented
   * letters like "Ä" or "É" are deliberately NOT taken into account: they reach another two rows
   * higher, and allowing for them would leave no room for the descenders at the height the tags
   * have. They are considered unlikely in the label of a meta tag, so they are allowed to touch the
   * upper edge. */
  private static InkRows measureInkRows(Font font) {
    return new InkRows(
      measureInkRows(font, "HWOSbdfhkl")[0],
      measureInkRows(font, "gjpqy")[1]);
  }

  /** @return the number of pixel rows the ink of the text covers above and below the baseline */
  private static int[] measureInkRows(Font font, String text) {
    // Room enough for the glyphs and their overhang, whatever the size of the font
    int baseline = 2 * font.getSize();
    BufferedImage image = new BufferedImage((text.length() + 1) * font.getSize(), 4 * font.getSize(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
    if (hints instanceof Map) {
      graphics.addRenderingHints((Map<?, ?>) hints);
    }
    graphics.setFont(font);
    graphics.drawString(text, 2, baseline);
    graphics.dispose();
    int firstRow = baseline;
    int lastRow = baseline - 1;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) >= INK_MIN_ALPHA) {
          firstRow = Math.min(firstRow, y);
          lastRow = Math.max(lastRow, y);
        }
      }
    }
    return new int[] { baseline - firstRow, lastRow - baseline + 1 };
  }

  private static final class InkRows {
    final int capitalRows;
    final int descenderRows;

    InkRows(int capitalRows, int descenderRows) {
      this.capitalRows = capitalRows;
      this.descenderRows = descenderRows;
    }
  }
}
