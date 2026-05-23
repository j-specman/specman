package specman.pdf;

import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import java.awt.*;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;

public class CircleShape extends Shape {
  private int radius;

  public CircleShape(Point center, int radius) {
    super(center);
    this.radius = radius;
    withBackgroundColor(DIAGRAMM_LINE_COLOR);
  }

  public boolean hasForm() { return true; }

  public void runPath(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    com.itextpdf.kernel.geom.Point pdfCenter = toPdfPoint(path.get(0), renderOffset, swing2pdfScaleFactor);
    pdfCanvas.circle(pdfCenter.getX(), pdfCenter.getY(), radius * swing2pdfScaleFactor);
  }

}
