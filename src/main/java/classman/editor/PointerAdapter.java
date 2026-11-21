package classman.editor;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Function;

/** Turns the mouse events of the left button into the calls of a {@link PointerListener}, in points of the diagram.
 * A drag only starts when the mouse has moved a few pixels, so that a click which shakes a little is no drag. */
class PointerAdapter extends MouseAdapter {
  private static final int DRAG_THRESHOLD = 3;

  private final Function<Point, Point2D> toDiagram;
  private final Consumer<Point> scrollTo;
  private PointerListener listener;
  private Point pressedAt;
  private boolean dragging;

  /** @param toDiagram maps a point of the canvas to the point of the diagram it shows
   * @param scrollTo makes the point of the canvas visible, as a drag may go beyond the window */
  PointerAdapter(Function<Point, Point2D> toDiagram, Consumer<Point> scrollTo) {
    this.toDiagram = toDiagram;
    this.scrollTo = scrollTo;
  }

  void setListener(PointerListener listener) {
    this.listener = listener;
  }

  boolean isDragging() { return dragging; }

  @Override
  public void mousePressed(MouseEvent e) {
    if (listener != null && SwingUtilities.isLeftMouseButton(e)) {
      pressedAt = e.getPoint();
      dragging = false;
      listener.pressed(toDiagram.apply(pressedAt));
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (pressedAt == null || (!dragging && pressedAt.distance(e.getPoint()) < DRAG_THRESHOLD)) {
      return;
    }
    dragging = true;
    scrollTo.accept(e.getPoint());
    listener.dragged(toDiagram.apply(e.getPoint()));
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (pressedAt == null) {
      return;
    }
    pressedAt = null;
    dragging = false;
    listener.released(toDiagram.apply(e.getPoint()));
  }
}
