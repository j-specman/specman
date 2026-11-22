package classman.editor;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Function;

/** Turns the mouse events of the left button into the calls of a {@link PointerListener}, in points of the diagram,
 * and a click which opens the context menu into a request for it.
 * A drag only starts when the mouse has moved a few pixels, so that a click which shakes a little is no drag. */
class PointerAdapter extends MouseAdapter {
  private static final int DRAG_THRESHOLD = 3;

  private final Function<Point, Point2D> toDiagram;
  private final Consumer<Point> scrollTo;
  private PointerListener listener;
  private Point pressedAt;
  private Point lastAt;
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

  /** Gives the component the cursor which the listener wants at the place where the mouse was last seen. While the
   * mouse is dragged the cursor stays as it is. */
  void updateCursor(Component component) {
    if (listener != null && lastAt != null && !dragging) {
      component.setCursor(listener.cursorAt(toDiagram.apply(lastAt)));
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    lastAt = e.getPoint();
    updateCursor(e.getComponent());
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (listener == null) {
      return;
    }
    lastAt = e.getPoint();
    if (e.isPopupTrigger()) {
      requestContextMenu(e);
    }
    else if (SwingUtilities.isLeftMouseButton(e)) {
      e.getComponent().requestFocusInWindow();
      if (e.getClickCount() > 1) {
        listener.doubleClicked(toDiagram.apply(e.getPoint()));
        return;
      }
      pressedAt = e.getPoint();
      dragging = false;
      listener.pressed(toDiagram.apply(pressedAt));
    }
  }

  /** The focus goes to the canvas first, so that a text which is typed is finished before the menu. */
  private void requestContextMenu(MouseEvent e) {
    e.getComponent().requestFocusInWindow();
    listener.contextMenuRequested(toDiagram.apply(e.getPoint()));
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (pressedAt == null || (!dragging && pressedAt.distance(e.getPoint()) < DRAG_THRESHOLD)) {
      return;
    }
    dragging = true;
    lastAt = e.getPoint();
    scrollTo.accept(e.getPoint());
    listener.dragged(toDiagram.apply(e.getPoint()));
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (listener != null && e.isPopupTrigger()) {
      requestContextMenu(e);
      return;
    }
    if (pressedAt == null) {
      return;
    }
    pressedAt = null;
    dragging = false;
    lastAt = e.getPoint();
    listener.released(toDiagram.apply(e.getPoint()));
    updateCursor(e.getComponent());
  }
}
