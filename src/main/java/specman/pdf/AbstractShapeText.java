package specman.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import specman.pdf.navigation.PdfNavigationRenderContext;

import java.awt.*;
import java.io.IOException;

abstract class AbstractShapeText {
  protected Insets insets;
  protected Color color;
  protected Font font;

  protected AbstractShapeText(Insets insets, Color color, Font font) {
    this.insets = insets;
    this.color = color;
    this.font = font;
  }

  public int getFontsize() {
    return font.getSize();
  }

  public Insets getInsets() {
    return insets;
  }

  public com.itextpdf.kernel.colors.Color getPDFColor() {
    return Shape.toPDFColor(color, Shape.DEFAULT_LINE_COLOR);
  }

  public abstract void writeToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document);

  public void writeToPDF(Point renderOffset, float scale, PdfCanvas canvas, Document document,
                         PdfNavigationRenderContext navigation, String textKey) {
    writeToPDF(renderOffset, scale, canvas, document);
  }

  public static PdfFont createFont(String fontProgram) {
    try {
      return PdfFontFactory.createFont(fontProgram);
    }
    catch(IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  protected abstract PdfFont getPDFFont();
}
