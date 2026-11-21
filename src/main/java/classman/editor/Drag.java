package classman.editor;

import java.awt.geom.Point2D;

/** A drag of something over the diagram, which changes the model as the mouse moves and can put it back. */
abstract class Drag {

  /** @return true once the first move has changed the model */
  abstract boolean hasMoved();

  /** Shows the dragged thing where it is when the mouse is at the given point. */
  abstract void to(Point2D at);

  /** Puts everything back where it was before the first move. */
  abstract void cancel();
}
