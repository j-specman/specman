package specman.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Draws an {@link Icon} as it looks on screen, e.g. in gray scale for the icon of a meta tag of a
 * deleted step: Swing paints the icon into a transparent image, which is embedded into the PDF.
 * The image is rendered in a multiple of the icon's size, so it still looks sharp in the PDF
 * viewer's zoom levels.
 */
public class IconShape extends Shape {
  private static final int OVERSAMPLING = 4;

  private final int width;
  private final int height;
  private final ImageData imageData;

  /** @param bounds the area the icon paints into, relative to the shape's parent. It is larger
   * than the icon itself if the icon paints itself at an offset. */
  public IconShape(Component component, Icon icon, java.awt.Rectangle bounds) {
    super(bounds.x, bounds.y);
    this.width = bounds.width;
    this.height = bounds.height;
    BufferedImage image = new BufferedImage(width * OVERSAMPLING, height * OVERSAMPLING, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.scale(OVERSAMPLING, OVERSAMPLING);
    icon.paintIcon(component, graphics, 0, 0);
    graphics.dispose();
    this.imageData = toImageData(image);
  }

  private static ImageData toImageData(BufferedImage image) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(image, "png", bytes);
      return ImageDataFactory.create(bytes.toByteArray());
    }
    catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /** The render offset is already translated to the top left corner of the icon's bounds. */
  @Override
  public void applyDecoration(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    Rectangle target = new Rectangle(
      renderOffset.x * swing2pdfScaleFactor,
      (renderOffset.y - height) * swing2pdfScaleFactor,
      width * swing2pdfScaleFactor,
      height * swing2pdfScaleFactor);
    pdfCanvas.addImageFittedIntoRectangle(imageData, target, false);
  }
}
