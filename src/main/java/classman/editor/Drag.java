package classman.editor;

import java.awt.geom.Point2D;

/** A drag of something over the diagram, which changes the model as the mouse moves and can put it back. */
abstract class Drag {

  /** @return true once the first move has changed the model */
  abstract boolean hasMoved();

  /** Shows the dragged thing where it is when the mouse is at the given point.
   * @param snapTolerance how close, in units of the diagram, the dragged thing has to come to something it can snap to
   * before it does */
  abstract void to(Point2D at, double snapTolerance);

  /** Puts everything back where it was before the first move. */
  abstract void cancel();
}
