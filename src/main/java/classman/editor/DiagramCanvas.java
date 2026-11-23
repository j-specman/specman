package classman.editor;

import classman.render.DiagramStyle;
import classman.render.Scene;
import classman.render.SceneGraphicsPainter;
import classman.render.SvgFrame;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Paints the scene of the diagram with Graphics2D. Mouse wheel with Ctrl zooms. If the diagram is smaller than the
 * window, the canvas fills the window, so there is room to drop things where the diagram has none yet. */
class DiagramCanvas extends JPanel implements Scrollable {
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.25;
  private static final double MAX_ZOOM = 4;
  private static final double ZOOM_ROUNDING_TOLERANCE = 1e-6;
  private static final int SCROLL_UNIT = 16;
  private static final int SELECTION_GAP = 4;
  private static final int SELECTION_WIDTH = 2;
  private static final int SELECTION_ALPHA = 190;
  private static final double HANDLE_DIAMETER = 9;
  private static final double PLACEHOLDER_DASH = 3;
  private static final int PLACEHOLDER_GAP = 2;
  private static final Color HANDLE_FILL = Color.WHITE;
  private static final Color DEFAULT_SELECTION_COLOR = new Color(0x2D7FF9);
  /** The zoom which the user chose last, for the canvases which are made from now on, as long as the application
   * runs. It belongs to the editor and has nothing to do with the zoom of the activity diagram. */
  private static double rememberedZoom = 1;

  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private Scene scene;
  private SelectionMarks marks = SelectionMarks.none();
  private double zoom = rememberedZoom;

  /** The point of the diagram at the top left of the canvas, and the size of the canvas in points of the diagram. The
   * origin is fixed: the point (0, 0) of the diagram stands at the same place of the canvas whatever the user moves,
   * and the user moves the visible part with the scroll bars only. It is the diagram's own top left corner minus a
   * margin, further out only if a diagram which has been made earlier reaches into negative coordinates. The size
   * follows the scene. */
  private double originX;
  private double originY;
  private boolean hasOrigin;
  private double extentWidth;
  private double extentHeight;
  private final PointerAdapter pointer = new PointerAdapter(this::toDiagram, this::showPoint);

  DiagramCanvas(Scene scene) {
    setBackground(Color.WHITE);
    setLayout(null);
    setFocusable(true);
    setScene(scene);
    setAutoscrolls(true);
    addMouseListener(pointer);
    addMouseMotionListener(pointer);
    addMouseWheelListener(e -> {
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
        zoomBy(e.getWheelRotation() < 0 ? ZOOM_STEP : 1 / ZOOM_STEP);
      }
      else {
        getParent().dispatchEvent(SwingUtilities.convertMouseEvent(this, e, getParent()));
      }
    });
  }

  void setScene(Scene scene) {
    this.scene = scene;
    SvgFrame frame = SvgFrame.of(scene);
    if (!hasOrigin) {
      originX = Math.min(frame.x, -DiagramStyle.SVG_MARGIN);
      originY = Math.min(frame.y, -DiagramStyle.SVG_MARGIN);
      hasOrigin = true;
    }
    extentWidth = Math.max(0, frame.x + frame.width - originX);
    extentHeight = Math.max(0, frame.y + frame.height - originY);
    revalidate();
    repaint();
  }

  /** Shows the cursor which the diagram wants where the mouse is, after something has changed without the mouse
   * being moved. */
  void refreshCursor() {
    pointer.updateCursor(this);
  }

  void setPointerListener(PointerListener listener) {
    pointer.setListener(listener);
  }

  private void showPoint(Point canvasPoint) {
    scrollRectToVisible(new Rectangle(canvasPoint.x, canvasPoint.y, 1, 1));
  }

  void setSelection(SelectionMarks marks) {
    this.marks = marks;
    repaint();
  }

  /** @return the point of the diagram which the given point of the canvas shows */
  Point2D toDiagram(Point canvasPoint) {
    return new Point2D.Double(canvasPoint.x / zoom + originX, canvasPoint.y / zoom + originY);
  }

  double zoom() { return zoom; }

  /** @return the length in the diagram which covers the given number of pixels of the screen */
  double toDiagramLength(double pixels) { return pixels / zoom; }

  /** @return the area of the canvas which shows the given area of the diagram */
  Rectangle toCanvas(Rectangle2D diagramArea) {
    return new Rectangle((int) Math.round((diagramArea.getX() - originX) * zoom),
      (int) Math.round((diagramArea.getY() - originY) * zoom),
      (int) Math.round(diagramArea.getWidth() * zoom), (int) Math.round(diagramArea.getHeight() * zoom));
  }

  /** Zooms in to the next multiple of 10 percent. */
  void zoomIn() {
    zoomTo((Math.floor(zoom * 10 + ZOOM_ROUNDING_TOLERANCE) + 1) / 10);
  }

  /** Zooms out to the previous multiple of 10 percent. */
  void zoomOut() {
    zoomTo((Math.ceil(zoom * 10 - ZOOM_ROUNDING_TOLERANCE) - 1) / 10);
  }

  private void zoomBy(double factor) {
    zoomTo(zoom * factor);
  }

  private void zoomTo(double newZoom) {
    zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
    rememberedZoom = zoom;
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension((int) Math.ceil(extentWidth * zoom), (int) Math.ceil(extentHeight * zoom));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.scale(zoom, zoom);
      g.translate(-originX, -originY);
      painter.paint(scene, g);
      paintSelection(g);
      paintConnectionLine(g);
      paintPlaceholders(g);
      paintLeaders(g);
      paintHandles(g);
    }
    finally {
      g.dispose();
    }
  }

  /** A translucent frame around each framed box, so what it covers shines through. It is as thick and as far from the
   * box on the screen at every zoom. */
  private void paintSelection(Graphics2D g) {
    double gap = SELECTION_GAP / zoom;
    g.setColor(selectionColor());
    g.setStroke(new BasicStroke((float) (SELECTION_WIDTH / zoom)));
    for (Rectangle2D frame : marks.frames) {
      g.draw(new Rectangle2D.Double(frame.getX() - gap, frame.getY() - gap,
        frame.getWidth() + 2 * gap, frame.getHeight() + 2 * gap));
    }
  }

  /** The line of a relation which is being drawn, as thick on the screen at every zoom as the frames. */
  private void paintConnectionLine(Graphics2D g) {
    if (marks.connectionLine != null) {
      g.setColor(selectionColor());
      g.setStroke(new BasicStroke((float) (SELECTION_WIDTH / zoom)));
      g.draw(marks.connectionLine);
    }
  }

  /** Dashed rectangles in the color of the selection where a selected relation could get a text, as thin and with as
   * long dashes on the screen at every zoom. While changes are tracked they are filled with the pale color of the
   * change set, like the field in which the text is typed. */
  private void paintPlaceholders(Graphics2D g) {
    float[] dashes = {(float) (PLACEHOLDER_DASH / zoom), (float) (PLACEHOLDER_DASH / zoom)};
    double gap = PLACEHOLDER_GAP / zoom;
    for (Rectangle2D placeholder : marks.placeholders) {
      Rectangle2D.Double outline = new Rectangle2D.Double(placeholder.getX() - gap, placeholder.getY() - gap,
        placeholder.getWidth() + 2 * gap, placeholder.getHeight() + 2 * gap);
      if (marks.placeholderFill != null) {
        g.setColor(marks.placeholderFill);
        g.fill(outline);
      }
      g.setColor(selectionColor());
      g.setStroke(new BasicStroke((float) (1 / zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashes, 0));
      g.draw(outline);
    }
  }

  /** Thin dashed lines in the color of the selection from the texts of a selected relation which stand far from its line
   * to the line, as thin and with as long dashes on the screen at every zoom. */
  private void paintLeaders(Graphics2D g) {
    float[] dashes = {(float) (PLACEHOLDER_DASH / zoom), (float) (PLACEHOLDER_DASH / zoom)};
    g.setColor(selectionColor());
    g.setStroke(new BasicStroke((float) (1 / zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashes, 0));
    for (Line2D leader : marks.leaders) {
      g.draw(leader);
    }
  }

  /** @return how far, in pixels of the screen, the dashed rectangle of a place for a missing text reaches out of the
   * area of the text */
  int placeholderMargin() { return PLACEHOLDER_GAP; }

  /** Small white circles outlined in the color of the selection, as large on the screen at every zoom. */
  private void paintHandles(Graphics2D g) {
    double diameter = HANDLE_DIAMETER / zoom;
    for (Point2D handle : marks.handles) {
      Ellipse2D circle = new Ellipse2D.Double(handle.getX() - diameter / 2, handle.getY() - diameter / 2,
        diameter, diameter);
      g.setColor(HANDLE_FILL);
      g.fill(circle);
      g.setColor(selectionColor());
      g.setStroke(new BasicStroke((float) (SELECTION_WIDTH / zoom)));
      g.draw(circle);
    }
  }

  private Color selectionColor() {
    if (marks.specialColor != null) {
      return withAlpha(marks.specialColor, SELECTION_ALPHA);
    }
    Color accent = UIManager.getColor("Component.focusColor");
    return withAlpha(accent != null ? accent : DEFAULT_SELECTION_COLOR, SELECTION_ALPHA);
  }

  private static Color withAlpha(Color color, int alpha) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return SCROLL_UNIT; }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return orientation == javax.swing.SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
  }
}
