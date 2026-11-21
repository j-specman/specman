package classman.editor;

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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Paints the scene of the diagram with Graphics2D. Mouse wheel with Ctrl zooms. If the diagram is smaller than the
 * window, the canvas fills the window, so there is room to drop things where the diagram has none yet. */
class DiagramCanvas extends JPanel implements Scrollable {
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.25;
  private static final double MAX_ZOOM = 4;
  private static final int SCROLL_UNIT = 16;
  private static final int SELECTION_GAP = 4;
  private static final int SELECTION_WIDTH = 2;
  private static final int SELECTION_ALPHA = 190;
  private static final double HANDLE_DIAMETER = 9;
  private static final Color HANDLE_FILL = Color.WHITE;
  private static final Color DEFAULT_SELECTION_COLOR = new Color(0x2D7FF9);

  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private Scene scene;
  private List<Rectangle2D> frames = List.of();
  private List<Point2D> handles = List.of();
  private Color specialSelectionColor;
  private double zoom = 1;

  /** The point of the diagram at the top left of the canvas, and the size of the canvas in points of the diagram.
   * They follow the scene, but not while a box is dragged: the scene starts at the top left box, and the canvas would
   * move along with the box and the mouse would never get ahead of it. */
  private double originX;
  private double originY;
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
    if (!pointer.isDragging()) {
      originX = frame.x;
      originY = frame.y;
    }
    extentWidth = Math.max(0, frame.x + frame.width - originX);
    extentHeight = Math.max(0, frame.y + frame.height - originY);
    revalidate();
    repaint();
  }

  void setPointerListener(PointerListener listener) {
    pointer.setListener(listener);
  }

  private void showPoint(Point canvasPoint) {
    scrollRectToVisible(new Rectangle(canvasPoint.x, canvasPoint.y, 1, 1));
  }

  /** @param frames the areas of the diagram which get a frame: the selected box, and the class which the end of a
   * relation is going to be hung onto
   * @param handles the points of a selected relation which are marked with small circles, empty if there are none
   * @param specialColor the color of the frames if the usual one can't be told from what is selected, null otherwise */
  void setSelection(List<Rectangle2D> frames, List<Point2D> handles, Color specialColor) {
    this.frames = List.copyOf(frames);
    this.handles = List.copyOf(handles);
    this.specialSelectionColor = specialColor;
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

  private void zoomBy(double factor) {
    zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
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
    for (Rectangle2D frame : frames) {
      g.draw(new Rectangle2D.Double(frame.getX() - gap, frame.getY() - gap,
        frame.getWidth() + 2 * gap, frame.getHeight() + 2 * gap));
    }
  }

  /** Small white circles outlined in the color of the selection, as large on the screen at every zoom. */
  private void paintHandles(Graphics2D g) {
    double diameter = HANDLE_DIAMETER / zoom;
    for (Point2D handle : handles) {
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
    if (specialSelectionColor != null) {
      return withAlpha(specialSelectionColor, SELECTION_ALPHA);
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
