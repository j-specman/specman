package specman.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import specman.editarea.image.AbstractImageEditArea;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** A picture which is available as bitmap only. */
public class ShapeBitmapImage extends ShapeImage {

  private final AbstractImageEditArea content;

  public ShapeBitmapImage(AbstractImageEditArea content) {
    this.content = content;
  }

  @Override
  public void drawToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    try {
      BufferedImage fullSizeImage = content.getFullSizeImage();
      float scaleFactor = content.getTotalScalePercent() * swing2pdfScaleFactor;
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(fullSizeImage, AbstractImageEditArea.PERSISTED_IMAGETYPE, bytes);
      ImageData data = ImageDataFactory.create(bytes.toByteArray());
      com.itextpdf.layout.element.Image img = new com.itextpdf.layout.element.Image(data);
      img.setAutoScale(true);

      Paragraph p = new Paragraph()
        .setFixedPosition(
          renderOffset.x * swing2pdfScaleFactor,
          (renderOffset.y - fullSizeImage.getHeight() * content.getTotalScalePercent()) * swing2pdfScaleFactor,
          fullSizeImage.getWidth() * scaleFactor)
        .setMargin(0);

      p.add(img);
      document.add(p);
    }
    catch(IOException iox) {
      throw new RuntimeException(iox);
    }
  }
}
