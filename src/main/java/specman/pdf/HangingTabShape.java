package specman.pdf;

import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants.LineCapStyle;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import specman.metatag.HangingTabRenderer;

import java.awt.Color;
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
  private Color borderColor;
  private float borderWidth;

  public HangingTabShape(Component component, int arc) {
    super(component);
    this.bounds = component.getBounds();
    this.radius = arc / 2f;
  }

  /** @param color the color of the border line, null for none
   * @param width the width of the border line in Swing pixels */
  public HangingTabShape withBorder(Color color, float width) {
    this.borderColor = color;
    this.borderWidth = width;
    return this;
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
   * can reuse toPdfX/toPdfY directly instead of duplicating its scale/flip math. */
  private void curveTo(Point start, Point control, Point end, Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    curveTo(start.x, start.y, control.x, control.y, end.x, end.y, renderOffset, swing2pdfScaleFactor, pdfCanvas);
  }

  private void curveTo(double startX, double startY, double controlX, double controlY, double endX, double endY,
                       Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    double p0x = toPdfX(startX, renderOffset, swing2pdfScaleFactor);
    double p0y = toPdfY(startY, renderOffset, swing2pdfScaleFactor);
    double qx = toPdfX(controlX, renderOffset, swing2pdfScaleFactor);
    double qy = toPdfY(controlY, renderOffset, swing2pdfScaleFactor);
    double p2x = toPdfX(endX, renderOffset, swing2pdfScaleFactor);
    double p2y = toPdfY(endY, renderOffset, swing2pdfScaleFactor);
    double c1x = p0x + (qx - p0x) * 2 / 3;
    double c1y = p0y + (qy - p0y) * 2 / 3;
    double c2x = p2x + (qx - p2x) * 2 / 3;
    double c2y = p2y + (qy - p2y) * 2 / 3;
    pdfCanvas.curveTo(c1x, c1y, c2x, c2y, p2x, p2y);
  }

  /** @return the opacity of the background color, which a meta tag may choose to be transparent */
  @Override
  public float getFillOpacity() {
    return backgroundColor != null ? backgroundColor.getAlpha() / 255f : 1f;
  }

  /** Draws the border along the left, bottom and right edge, but not the top, like the border of
   * an {@link specman.metatag.AbstractMetaTag} on screen. The renderOffset is already translated
   * to the top left corner of the shape. */
  @Override
  public void applyDecoration(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    if (borderColor == null) {
      return;
    }
    // Inset by half the line width, so the (centered-on-path) line stays within the bounds.
    double inset = borderWidth / 2;
    double left = inset;
    double right = bounds.width - inset;
    double bottom = bounds.height - inset;
    double r = radius;

    pdfCanvas.saveState();
    pdfCanvas.setStrokeColor(toPDFColor(borderColor, DEFAULT_LINE_COLOR));
    pdfCanvas.setExtGState(new PdfExtGState().setStrokeOpacity(borderColor.getAlpha() / 255f));
    pdfCanvas.setLineWidth(borderWidth * swing2pdfScaleFactor);
    // The renderer's projecting square caps would let the line stick out above the top edge
    pdfCanvas.setLineCapStyle(LineCapStyle.BUTT);
    pdfCanvas.moveTo(toPdfX(left, renderOffset, swing2pdfScaleFactor), toPdfY(0.0, renderOffset, swing2pdfScaleFactor));
    pdfCanvas.lineTo(toPdfX(left, renderOffset, swing2pdfScaleFactor), toPdfY(bottom - r, renderOffset, swing2pdfScaleFactor));
    curveTo(left, bottom - r, left, bottom, left + r, bottom, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    pdfCanvas.lineTo(toPdfX(right - r, renderOffset, swing2pdfScaleFactor), toPdfY(bottom, renderOffset, swing2pdfScaleFactor));
    curveTo(right - r, bottom, right, bottom, right, bottom - r, renderOffset, swing2pdfScaleFactor, pdfCanvas);
    pdfCanvas.lineTo(toPdfX(right, renderOffset, swing2pdfScaleFactor), toPdfY(0.0, renderOffset, swing2pdfScaleFactor));
    pdfCanvas.stroke();
    pdfCanvas.restoreState();
  }
}
