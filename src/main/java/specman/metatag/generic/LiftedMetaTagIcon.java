package specman.metatag.generic;

import specman.graphics.SvgIcon;

import java.awt.Component;
import java.awt.Graphics;

import static specman.Specman.editor;

/** The icon of a {@link GenericMetaTag}: an {@link SvgIcon} in the tag's glyph size, painted a
 * little lower than Swing places it. The tag lifts the centered content of its label
 * (icon and text) by its bottom inset; this icon compensates for that, see {@link #setLift(int)},
 * so it stays where it was, and only the text moves up. */
class LiftedMetaTagIcon extends SvgIcon {
  private static final int GLYPH_SIZE = 10;

  private boolean grayedOut = false;
  private int lift = 0;

  LiftedMetaTagIcon(String svgContent) {
    super(GLYPH_SIZE, svgContent);
    scale();
  }

  /** Adapts the icon to the current zoom factor. */
  void scale() {
    setSize((int) editor().scale(GLYPH_SIZE));
  }

  /** @param lift the number of pixels the icon is painted lower than where Swing places it */
  void setLift(int lift) {
    this.lift = lift;
  }

  int lift() {
    return lift;
  }

  /** Paints the icon in gray scale, e.g. for a tag at a deleted step. */
  void setGrayedOut(boolean grayedOut) {
    this.grayedOut = grayedOut;
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    int liftedY = y + lift();
    if (grayedOut) {
      getDisabledIcon().paintIcon(component, graphics, x, liftedY);
    }
    else {
      super.paintIcon(component, graphics, x, liftedY);
    }
  }
}
