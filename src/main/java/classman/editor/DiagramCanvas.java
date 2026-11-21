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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
  private static final Color DEFAULT_SELECTION_COLOR = new Color(0x2D7FF9);

  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private Scene scene;
  private Rectangle2D selection;
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

  /** @param area the area of the diagram which is selected, null if nothing is
   * @param specialColor the color of the frame if the usual one can't be told from what is selected, null otherwise */
  void setSelection(Rectangle2D area, Color specialColor) {
    this.selection = area;
    this.specialSelectionColor = specialColor;
    repaint();
  }

  /** @return the point of the diagram which the given point of the canvas shows */
  Point2D toDiagram(Point canvasPoint) {
    return new Point2D.Double(canvasPoint.x / zoom + originX, canvasPoint.y / zoom + originY);
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
    }
    finally {
      g.dispose();
    }
  }

  /** A translucent frame around the selected box, so what it covers shines through. It is as thick and as far from
   * the box on the screen at every zoom. */
  private void paintSelection(Graphics2D g) {
    if (selection == null) {
      return;
    }
    double gap = SELECTION_GAP / zoom;
    g.setColor(selectionColor());
    g.setStroke(new BasicStroke((float) (SELECTION_WIDTH / zoom)));
    g.draw(new Rectangle2D.Double(selection.getX() - gap, selection.getY() - gap,
      selection.getWidth() + 2 * gap, selection.getHeight() + 2 * gap));
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
