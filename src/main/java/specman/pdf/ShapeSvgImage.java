package specman.pdf;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.svg.converter.SvgConverter;
import com.itextpdf.svg.processors.impl.SvgConverterProperties;
import specman.editarea.AbstractImageEditArea;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/** A picture which is available as SVG. It is drawn as vector graphic with the texts set in Specman's embedded fonts. */
public class ShapeSvgImage extends ShapeImage {

  private final AbstractImageEditArea content;
  private final byte[] svg;
  /** The size of the picture at 100%, in Swing pixels. */
  private final int width;
  private final int height;

  public ShapeSvgImage(AbstractImageEditArea content, byte[] svg, int width, int height) {
    this.content = content;
    this.svg = svg;
    this.width = width;
    this.height = height;
  }

  @Override
  public void drawToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    try {
      SvgConverterProperties properties = new SvgConverterProperties();
      properties.setFontProvider(FormattedShapeText.fontProvider);
      PdfFormXObject picture = SvgConverter.convertToXObject(new ByteArrayInputStream(svg), document.getPdfDocument(), properties);
      float scale = content.getTotalScalePercent();
      pdfCanvas.addXObjectFittedIntoRectangle(picture, new Rectangle(
        renderOffset.x * swing2pdfScaleFactor,
        (renderOffset.y - height * scale) * swing2pdfScaleFactor,
        width * scale * swing2pdfScaleFactor,
        height * scale * swing2pdfScaleFactor));
    }
    catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }
}
