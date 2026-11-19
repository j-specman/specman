package specman.editarea.image.classman;

import classman.render.Scene;
import classman.render.SceneGraphicsPainter;
import classman.render.SvgFrame;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;

/** Paints a Classman scene with Graphics2D. Mouse wheel with Ctrl zooms. */
class ClassmanCanvas extends JPanel {
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.25;
  private static final double MAX_ZOOM = 4;

  private final Scene scene;
  private final SvgFrame frame;
  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private double zoom = 1;

  ClassmanCanvas(Scene scene) {
    this.scene = scene;
    this.frame = SvgFrame.of(scene);
    setBackground(Color.WHITE);
    addMouseWheelListener(e -> {
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
        zoomBy(e.getWheelRotation() < 0 ? ZOOM_STEP : 1 / ZOOM_STEP);
      }
      else {
        getParent().dispatchEvent(SwingUtilities.convertMouseEvent(this, e, getParent()));
      }
    });
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
}
