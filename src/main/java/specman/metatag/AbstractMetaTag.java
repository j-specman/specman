package specman.metatag;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import static specman.Specman.editor;

/**
 * Common base for meta-tag widgets (step number label, fold/collapse button, future ones) that
 * render as if hanging from the top edge of their container: sharp top corners, antialiased
 * rounded bottom corners (see {@link HangingTabRenderer} for how). Subclasses supply the corner arc
 * via {@link #arc()} and can override {@link #paintUnmasked(Graphics)} for content that must not
 * be clipped by the rounded-bottom mask (e.g. StepnumberLabel's deletion strike-through line,
 * painted directly onto the final Graphics after HangingTabShape has already composited the
 * masked content).
 * <p>
 * {@link #setBorderColor(Color)} draws a thin, antialiased border along the bottom/left/right
 * edges only (not the top, matching the "hanging from above" look) directly in paint() - not via
 * a Swing {@link javax.swing.border.Border}, so it never affects the widget's size the way a real
 * Border's insets would, and doesn't fight with HangingTabShape's own offscreen compositing the
 * way a MatteBorder painted as part of super.paint() used to (causing visible render glitches).
 */
public abstract class AbstractMetaTag extends JLabel {
  // Base (100% zoom) corner radius for the rounded bottom corners - shared default for arc(), at
  // 100% zoom, scaled the same way the rest of a widget's own size is.
  protected static final int CORNER_ARC = 8;
  // Extra width left/right of a widget's own content (text or icon), so the rounded corners have
  // room to live in without biting into the content itself. Not zoom-scaled, matching the
  // existing (pre-meta-strip) border insets on StepnumberLabel, which weren't either.
  private static final int SIDE_PADDING = 5;
  // Border bottom inset that nudges Swing's centered content upward within its natural preferred
  // height (a larger bottom inset than top shifts the centering point up).
  protected static final int CONTENT_LIFT = 2;

  private Color borderColor;

  protected AbstractMetaTag() {
  }

  protected AbstractMetaTag(String text) {
    super(text);
  }

  public void setBorderColor(Color borderColor) {
    this.borderColor = borderColor;
  }

  /** Border recipe for an AbstractMetaTag's own content box: SIDE_PADDING left/right for the rounded
   * corners to live in, CONTENT_LIFT bottom inset to nudge Swing's centered content upward,
   * reduced by heightGrowthCompensation to counteract a matching height growth in "abgesetzte"
   * (rounded-border) steps (see StepnumberLabel#setHeightGrowthCompensation and
   * MetaTagPanel.BORDER_OVERDRAW_COMPENSATION for why that's needed there). */
  protected static Border scaledBorder(Color color, int heightGrowthCompensation) {
    return scaledBorder(color, heightGrowthCompensation, SIDE_PADDING);
  }

  /** Same as {@link #scaledBorder(Color, int)}, but with an explicit side padding instead of the
   * default SIDE_PADDING - for widgets (like KlappButton's plus/minus) that are known not to
   * collide with the rounded bottom corners even with much less padding than StepnumberLabel's
   * digits need, and so can sit closer to their own edges. */
  protected static Border scaledBorder(Color color, int heightGrowthCompensation, int sidePadding) {
    return scaledBorder(color, heightGrowthCompensation, sidePadding, CONTENT_LIFT);
  }

  /** Same as {@link #scaledBorder(Color, int, int)}, but with an explicit lift instead of the
   * default CONTENT_LIFT. */
  protected static Border scaledBorder(Color color, int heightGrowthCompensation, int sidePadding, int lift) {
    int padding = (int) editor().scale(sidePadding);
    int scaledLift = (int) editor().scale(lift) - heightGrowthCompensation;
    return new MatteBorder(0, padding, scaledLift, padding, color);
  }

  @Override
  public void paint(Graphics g) {
    HangingTabRenderer.paint(this, g, arc(), super::paint);
    paintUnmasked(g);
    paintHangingTabBorder(g);
  }

  private void paintHangingTabBorder(Graphics g) {
    if (borderColor == null) {
      return;
    }
    float strokeWidth = borderStrokeWidth();
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(borderColor);
    g2.setStroke(new BasicStroke(strokeWidth));
    // Inset by half the stroke width so the (centered-on-path) stroke stays fully within the
    // widget's own bounds instead of half of it getting clipped off at the edges.
    g2.translate(strokeWidth / 2, strokeWidth / 2);
    g2.draw(HangingTabRenderer.outline(getWidth() - strokeWidth, getHeight() - strokeWidth, arc()));
    g2.dispose();
  }

  protected abstract int arc();

  /** Width (in pixels, at the widget's current size) of the border line drawn by
   * setBorderColor() - 1px at 100% zoom by default; override to scale with zoom like arc()
   * already does, so the line doesn't look disproportionately thick at higher zoom levels. */
  protected float borderStrokeWidth() {
    return 1f;
  }

  protected void paintUnmasked(Graphics g) {
  }
}
