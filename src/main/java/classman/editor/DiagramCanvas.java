package classman.editor;

import classman.render.Scene;
import classman.render.SceneGraphicsPainter;
import classman.render.SvgFrame;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;

/** Paints the scene of the diagram with Graphics2D. Mouse wheel with Ctrl zooms. If the diagram is smaller than the
 * window, the canvas fills the window, so there is room to drop things where the diagram has none yet. */
class DiagramCanvas extends JPanel implements Scrollable {
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.25;
  private static final double MAX_ZOOM = 4;
  private static final int SCROLL_UNIT = 16;

  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private Scene scene;
  private SvgFrame frame;
  private double zoom = 1;

  DiagramCanvas(Scene scene) {
    setBackground(Color.WHITE);
    setScene(scene);
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
    }
    finally {
      g.dispose();
    }
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
