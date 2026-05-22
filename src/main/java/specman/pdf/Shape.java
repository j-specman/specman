package specman.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants.LineJoinStyle;
import specman.styles.Styles;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;
import static specman.view.AbstractSchrittView.LINIENBREITE;

public class Shape {
  public static final com.itextpdf.kernel.colors.Color DEFAULT_FILL_COLOR = DeviceRgb.WHITE;
  public static final com.itextpdf.kernel.colors.Color DEFAULT_LINE_COLOR = DeviceRgb.BLACK;
  public static final Color GAP_COLOR = DIAGRAMM_LINE_COLOR;

  protected List<Point> path = new ArrayList<>();
  protected Color backgroundColor;
  private List<Shape> subshapes = new ArrayList<>();
  private Object source;
  private boolean withOutline;
  private AbstractShapeText text;
  private ShapeImage image;
  protected float lineWidth = (float)LINIENBREITE;

  public Shape() {}

  public Shape(Component component) {
    this(component, null);
  }

  public Shape(Component component, Object source) {
    this.source = source;
    Rectangle r = component.getBounds();
    add(r.x, r.y)
      .add(r.x + r.width, r.y)
      .add(r.x + r.width, r.y + r.height)
      .add(r.x, r.y + r.height);
    backgroundColor = component.getBackground();
  }

  public Shape(int startX, int startY) {
    add(startX, startY);
  }

  public Shape(Point start) {
    add(start.x, start.y);
  }

  public Shape withBackgroundColor(Color color) {
    this.backgroundColor = color;
    return this;
  }

  public Shape add(int x, int y) {
    return add(new Point(x, y));
  }

  public Shape add(Point point) {
    path.add(point);
    return this;
  }

  public Point start() {
    return path.isEmpty() ? null : path.get(0);
  }

  public List<Point> allButStart() { return path.subList(1, path.size()); }

  public Shape add(Shape subshape) {
    if (subshape != null) {
      subshapes.add(subshape);
    }
    return this;
  }

  public Shape add(List<? extends Shape> subshapes) {
    this.subshapes.addAll(subshapes);
    return this;
  }

  public Shape add(Component subComponent) {
    return (subComponent != null)
      ? add(new Shape(subComponent, subComponent))
      : this;
  }

  public List<Shape> getSubshapes() { return subshapes; }

  public Point translate(Point renderOffset) {
    Point translatedOffset = new Point(renderOffset);
    translatedOffset.translate(start().x, -start().y);
    return translatedOffset;
  }

  public boolean isLine() { return path.size() == 2; }

  public int[] xPositionsAsArray() {
    return path.stream().mapToInt(point -> point.x).toArray();
  }

  public int[] yPositionsAsArray() {
    return path.stream().mapToInt(point -> point.y).toArray();
  }

  public com.itextpdf.kernel.colors.Color getPDFBackgroundColor() {
    return toPDFColor(backgroundColor, DEFAULT_FILL_COLOR);
  }

  static com.itextpdf.kernel.colors.Color toPDFColor(Color awtColor, com.itextpdf.kernel.colors.Color fallback) {
    if (awtColor == null) {
      return fallback;
    }
    return new DeviceRgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
  }

  public boolean hasForm() { return path.size() > 1; }

  public boolean withOutline() {
    return withOutline || isLine();
  }

  public Shape withOutline(boolean withOutline) {
    this.withOutline = withOutline;
    return this;
  }

  public Shape withText(AbstractShapeText text) {
    this.text = text;
    return this;
  }

  public AbstractShapeText getText() {
    return text;
  }

  /** This is very primitive yet - should be improved! */
  public int getWidth() {
    if (hasForm()) {
      return path.stream().mapToInt(p -> p.x).max().orElse(0);
    }
    return subshapes.stream().mapToInt(s -> s.getWidth()).max().orElse(0);
  }

  public int getHeight() {
    if (hasForm()) {
      return path.stream().mapToInt(p -> p.y).max().orElse(0);
    }
    return subshapes.stream().mapToInt(s -> s.getHeight()).max().orElse(0);
  }

  public Shape withImage(ShapeImage shapeImage) {
    this.image = shapeImage;
    return this;
  }

  public ShapeImage getImage() {
    return image;
  }

  public void runPath(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    moveTo(start(), renderOffset, swing2pdfScaleFactor, pdfCanvas);
    allButStart().forEach(p -> lineTo(p, renderOffset, swing2pdfScaleFactor, pdfCanvas));
    lineTo(start(), renderOffset, swing2pdfScaleFactor, pdfCanvas);
  }

  protected PdfCanvas moveTo(Point p, Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    com.itextpdf.kernel.geom.Point pdfPoint = toPdfPoint(p, renderOffset, swing2pdfScaleFactor);
    return pdfCanvas.moveTo(pdfPoint.getX(), pdfPoint.getY());
  }

  protected PdfCanvas lineTo(Point p, Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
    com.itextpdf.kernel.geom.Point pdfPoint = toPdfPoint(p, renderOffset, swing2pdfScaleFactor);
    return pdfCanvas.lineTo(pdfPoint.getX(), pdfPoint.getY());
  }

  protected com.itextpdf.kernel.geom.Point toPdfPoint(Point p, Point renderOffset, float swing2pdfScaleFactor) {
    float x = toPdfX(p.x, renderOffset, swing2pdfScaleFactor);
    float y = toPdfY(p.y, renderOffset, swing2pdfScaleFactor);
    return new com.itextpdf.kernel.geom.Point(x, y);
  }

  protected float toPdfX(int x, Point renderOffset, float swing2pdfScaleFactor) {
    return (x + renderOffset.x)*swing2pdfScaleFactor;
  }

  protected float toPdfY(int y, Point renderOffset, float swing2pdfScaleFactor) {
    return (renderOffset.y - y)*swing2pdfScaleFactor;
  }

  protected float toPdfX(double x, Point renderOffset, float swing2pdfScaleFactor) {
    return ((float)x + renderOffset.x)*swing2pdfScaleFactor;
  }

  protected float toPdfY(double y, Point renderOffset, float swing2pdfScaleFactor) {
    return (renderOffset.y - (float)y)*swing2pdfScaleFactor;
  }

  public void applyDecoration(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas) {
  }

  public float lineWidth() { return lineWidth; }
}
