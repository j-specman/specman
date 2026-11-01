package specman.pdf;

import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;

import java.awt.Point;

/** The picture of an image edit area, drawn into the PDF. */
public abstract class ShapeImage {

  public abstract void drawToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document);
}
