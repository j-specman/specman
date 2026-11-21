package classman.editor;

import java.awt.geom.Point2D;

/** What the mouse does on the canvas, in points of the diagram. */
interface PointerListener {
  void pressed(Point2D at);

  /** Only called after the mouse has moved a little away from where it was pressed. */
  void dragged(Point2D at);

  void released(Point2D at);

  /** Called instead of {@link #pressed} for the second press of a double click. */
  void doubleClicked(Point2D at);
}
