package specman.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfSimpleFont;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontCharacteristics;
import com.itextpdf.layout.font.FontInfo;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

import static specman.pdf.FormattedShapeText.fontProvider;

public class LabelShapeText extends AbstractShapeText {

  private String content;

  public LabelShapeText(String content, Insets insets, Color color, Font font) {
    super(insets, color, font);
    this.content = content;
  }

  public void writeToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    float scaledFontSize = getFontsize() * swing2pdfScaleFactor;
    pdfCanvas.setFillColor(getPDFColor());
    pdfCanvas.beginText().setFontAndSize(getPDFFont(), scaledFontSize)
      .moveText(
        (renderOffset.x + insets.left -1) * swing2pdfScaleFactor,
        (renderOffset.y - insets.top +1) * swing2pdfScaleFactor - scaledFontSize)
      .showText(getContent())
      .endText();
  }

  public String getContent() { return content; }

  @Override
  protected PdfFont getPDFFont() {
    String family = font.getFamily().replace(" ", "");
    if (family.contains("Sans")) {
      family = "Helvetica";
    }
    FontInfo info = fontProvider.getFontSelector(Arrays.asList(family), null).bestMatch();
    return fontProvider.getPdfFont(info);
  }
}
