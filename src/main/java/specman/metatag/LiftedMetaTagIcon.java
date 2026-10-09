package specman.metatag;

import specman.graphics.SvgIcon;

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

  LiftedMetaTagIcon(String iconBasename) {
    super(iconBasename, GLYPH_SIZE);
    scale();
  }

  /** Adapts the icon to the current zoom factor. */
  void scale() {
    setSize((int) editor().scale(GLYPH_SIZE));
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    super.paintIcon(component, graphics, x, y + (int) editor().scale(LIFT));
  }
}
