package specman.editarea.image;

import specman.ChangeInfo;
import specman.pdf.ShapeImage;
import specman.pdf.ShapeSvgImage;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** An edit area showing a vector picture, which stays sharp at any zoom level and is a vector graphic in the PDF,
 * where it is placed as SVG. Subclasses say which picture to show, see {@link #showPicture(Picture)}. */
public abstract class AbstractVectorImageEditArea extends AbstractImageEditArea {
  private Picture picture;

  protected AbstractVectorImageEditArea(ChangeInfo changeInfo, float individualScalePercent) {
    super(changeInfo, individualScalePercent);
  }

  /** Subclasses call this to replace the picture. It is not shown before {@link #imageChanged()}. */
  protected void showPicture(Picture picture) {
    this.picture = picture;
  }

  @Override
  protected ShapeImage createShapeImage() {
    return new ShapeSvgImage(this, picture.svg(), picture.width(), picture.height());
  }

  @Override
  protected int intrinsicWidth() { return picture.width(); }

  @Override
  protected Icon createScaledIcon(float totalScalePercent) { return picture.scaledIcon(totalScalePercent); }

  @Override
  public BufferedImage getFullSizeImage() {
    BufferedImage bitmap = new BufferedImage(picture.width(), picture.height(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bitmap.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      picture.scaledIcon(1).paintIcon(new JLabel(), g, 0, 0);
    }
    finally {
      g.dispose();
    }
    return bitmap;
  }
}
