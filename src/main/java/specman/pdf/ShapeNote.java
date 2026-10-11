package specman.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation;

import java.awt.Point;

/**
 * A comment attached to an area of a {@link Shape}, e.g. the text of a label. The PDF viewer shows
 * the area highlighted, with a small marker which reveals the comment when it is hovered or clicked.
 */
public class ShapeNote {
  private static final DeviceRgb HIGHLIGHT_COLOR = new DeviceRgb(255, 235, 120);

  private final String title;
  private final String text;
  private final java.awt.Rectangle area;

  /** @param title shown by the viewer as the author of the note
   * @param area the area the note is attached to, in Swing pixels relative to the shape */
  public ShapeNote(String title, String text, java.awt.Rectangle area) {
    this.title = title;
    this.text = text;
    this.area = area;
  }

  /** @param renderOffset the offset translated to the top left corner of the shape the note belongs to
   * @return the area of the note on the page */
  Rectangle placeOnPage(Point renderOffset, float swing2pdfScaleFactor) {
    return new Rectangle(
      (renderOffset.x + area.x) * swing2pdfScaleFactor,
      (renderOffset.y - area.y - area.height) * swing2pdfScaleFactor,
      area.width * swing2pdfScaleFactor,
      area.height * swing2pdfScaleFactor);
  }

  PdfAnnotation toAnnotation(Rectangle areaOnPage) {
    float left = areaOnPage.getLeft();
    float right = areaOnPage.getRight();
    float bottom = areaOnPage.getBottom();
    float top = areaOnPage.getTop();
    // The corners in the order upper left, upper right, lower left, lower right
    float[] quadPoints = { left, top, right, top, left, bottom, right, bottom };
    return PdfTextMarkupAnnotation.createHighLight(areaOnPage, quadPoints)
      .setColor(HIGHLIGHT_COLOR)
      .setTitle(new PdfString(title))
      .setContents(text);
  }
}
