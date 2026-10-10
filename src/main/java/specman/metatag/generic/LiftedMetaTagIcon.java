package specman.metatag.generic;

import specman.graphics.SvgIcon;
import specman.metatag.AbstractMetaTag;

import java.awt.Component;
import java.awt.Graphics;

import static specman.Specman.editor;

/** The icon of a {@link GenericMetaTag}: an {@link SvgIcon} in the tag's glyph size, painted a
 * little lower than Swing places it. AbstractMetaTag lifts the centered content of its label
 * (icon and text) by half of CONTENT_LIFT via an asymmetric border; this icon compensates for
 * that so it stays where it was, and only the text moves up. */
class LiftedMetaTagIcon extends SvgIcon {
  private static final int GLYPH_SIZE = 10;
  private static final int LIFT = AbstractMetaTag.CONTENT_LIFT / 2;

  private boolean grayedOut = false;

  LiftedMetaTagIcon(String svgContent) {
    super(GLYPH_SIZE, svgContent);
    scale();
  }

  /** Adapts the icon to the current zoom factor. */
  void scale() {
    setSize((int) editor().scale(GLYPH_SIZE));
  }

  /** Paints the icon in gray scale, e.g. for a tag at a deleted step. */
  void setGrayedOut(boolean grayedOut) {
    this.grayedOut = grayedOut;
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    int liftedY = y + (int) editor().scale(LIFT);
    if (grayedOut) {
      getDisabledIcon().paintIcon(component, graphics, x, liftedY);
    }
    else {
      super.paintIcon(component, graphics, x, liftedY);
    }
  }
}
