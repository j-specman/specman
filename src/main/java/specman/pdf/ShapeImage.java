package specman.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import specman.editarea.ImageEditArea;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ShapeImage {

  private ImageEditArea content;

  public ShapeImage(ImageEditArea content) {
    this.content = content;
  }

  public void drawToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    try {
      BufferedImage fullSizeImage = content.getFullSizeImage();
      float scaleFactor = content.getTotalScalePercent() * swing2pdfScaleFactor;
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(fullSizeImage, ImageEditArea.PERSISTED_IMAGETYPE, bytes);
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
