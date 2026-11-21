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
import java.util.function.Consumer;

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
  private SvgFrame frame;
  private Rectangle2D selection;
  private double zoom = 1;

  /** @param pressedAt called with the point of the diagram where the left mouse button was pressed */
  DiagramCanvas(Scene scene, Consumer<Point2D> pressedAt) {
    setBackground(Color.WHITE);
    setScene(scene);
    addMouseListener(new PressAdapter(e -> pressedAt.accept(toDiagram(e.getPoint()))));
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
    this.frame = SvgFrame.of(scene);
    revalidate();
    repaint();
  }

  /** @param area the area of the diagram which is selected, null if nothing is */
  void setSelection(Rectangle2D area) {
    this.selection = area;
    repaint();
  }

  /** @return the point of the diagram which the given point of the canvas shows */
  Point2D toDiagram(Point canvasPoint) {
    return new Point2D.Double(canvasPoint.x / zoom + frame.x, canvasPoint.y / zoom + frame.y);
  }

  private void zoomBy(double factor) {
    zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension((int) Math.ceil(frame.width * zoom), (int) Math.ceil(frame.height * zoom));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.scale(zoom, zoom);
      g.translate(-frame.x, -frame.y);
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

  private static Color selectionColor() {
    Color accent = UIManager.getColor("Component.focusColor");
    Color strong = accent != null ? accent : DEFAULT_SELECTION_COLOR;
    return new Color(strong.getRed(), strong.getGreen(), strong.getBlue(), SELECTION_ALPHA);
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
