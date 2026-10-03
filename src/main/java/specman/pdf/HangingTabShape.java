package specman.pdf;

import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import specman.graphics.HangingTabRenderer;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Like the plain rectangle from {@link Shape#Shape(Component)}, but with the bottom two corners
 * rounded and the top two left sharp - the PDF-side counterpart of
 * {@link HangingTabRenderer}'s on-screen "hanging tab" mask, used for the step
 * number label's own background fill so it doesn't print as a plain, unrounded box.
 */
public class HangingTabShape extends Shape {
  private final Rectangle bounds;
  // RoundRectangle2D's own arc parameter (see HangingTabShape) is the corner's full diameter, not
  // its radius - halved here once so the rest of this class can just work with a radius.
  private final float radius;

  public HangingTabShape(Component component, int arc) {
    super(component);
    this.bounds = component.getBounds();
    this.radius = arc / 2f;
  }

  @Override
  public void runPath(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    int left = bounds.x;
    int right = bounds.x + bounds.width;
    int top = bounds.y;
    int bottom = bounds.y + bounds.height;
    int r = Math.round(radius);

    Point topLeft = new Point(left, top);
    Point rightBeforeCurve = new Point(right, bottom - r);
    Point bottomRightCorner = new Point(right, bottom);
    Point bottomAfterRightCurve = new Point(right - r, bottom);
    Point bottomBeforeLeftCurve = new Point(left + r, bottom);
    Point bottomLeftCorner = new Point(left, bottom);
    Point leftAfterCurve = new Point(left, bottom - r);

    moveTo(topLeft, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    lineTo(new Point(right, top), renderOffset, swing2pdfScaleFactor, pdfCanvas);
    lineTo(rightBeforeCurve, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    curveTo(rightBeforeCurve, bottomRightCorner, bottomAfterRightCurve, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    lineTo(bottomBeforeLeftCurve, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    curveTo(bottomBeforeLeftCurve, bottomLeftCorner, leftAfterCurve, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    lineTo(topLeft, renderOffset, swing2pdfScaleFactor, pdfCanvas);
  }

  /** Draws a corner from start to end, using control as the sharp-cornered quadratic Bezier
   * control point - the same construction HangingTabShape itself uses on screen - converted to
   * the cubic Bezier iText's curveTo expects via the standard, exact (not approximated)
   * quadratic-to-cubic formula: C1 = P0 + 2/3*(Q-P0), C2 = P2 + 2/3*(Q-P2). Converting the three
   * points to PDF space first and interpolating there is equivalent to interpolating in Swing
   * space first (the 2/3 interpolation is affine, and so is the Swing-to-PDF conversion), so this
   * can reuse toPdfPoint() directly instead of duplicating its scale/flip math. */
  private void curveTo(Point start, Point control, Point end, Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    com.itextpdf.kernel.geom.Point p0 = toPdfPoint(start, renderOffset, swing2pdfScaleFactor);
    com.itextpdf.kernel.geom.Point q = toPdfPoint(control, renderOffset, swing2pdfScaleFactor);
    com.itextpdf.kernel.geom.Point p2 = toPdfPoint(end, renderOffset, swing2pdfScaleFactor);
    double c1x = p0.getX() + (q.getX() - p0.getX()) * 2 / 3;
    double c1y = p0.getY() + (q.getY() - p0.getY()) * 2 / 3;
    double c2x = p2.getX() + (q.getX() - p2.getX()) * 2 / 3;
    double c2y = p2.getY() + (q.getY() - p2.getY()) * 2 / 3;
    pdfCanvas.curveTo(c1x, c1y, c2x, c2y, p2.getX(), p2.getY());
  }
}
