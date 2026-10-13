package specman.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

public class LabelShapeText extends AbstractShapeText {

  private String content;

  public LabelShapeText(String content, Insets insets, Color color, Font font) {
    super(insets, color, font);
    this.content = content;
  }

  /** The text of the label at the position where Swing paints it.
   * @param textRect the area the label's layout computation puts the text into */
  public static LabelShapeText at(JLabel label, Rectangle textRect) {
    // writeToPDF() puts the baseline (top inset + font size) below the top
    int ascent = label.getFontMetrics(label.getFont()).getAscent();
    Insets insets = new Insets(textRect.y + ascent - label.getFont().getSize(), textRect.x + 1, 0, 0);
    return new LabelShapeText(label.getText(), insets, label.getForeground(), label.getFont());
  }

  public void writeToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    float scaledFontSize = getFontsize() * swing2pdfScaleFactor;
    pdfCanvas.setFillColor(getPDFColor());
    pdfCanvas.beginText().setFontAndSize(getPDFFont(), scaledFontSize)
      .moveText(
        (renderOffset.x + insets.left -1) * swing2pdfScaleFactor,
        (renderOffset.y - insets.top) * swing2pdfScaleFactor - scaledFontSize)
      .showText(getContent())
      .endText();
  }

  public String getContent() { return content; }

  @Override
  protected PdfFont getPDFFont() {
    return FormattedShapeText.resolvePdfFont(font);
  }
}
