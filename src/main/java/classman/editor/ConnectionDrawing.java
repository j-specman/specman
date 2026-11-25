package classman.editor;

import classman.render.SceneRelationLine;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** A connection which the user is drawing from a note, a class or an attribute to a class, an attribute or a
 * relation, before it is created: a relation from a class, an anchor from a note, a dependency from a class or attribute.
 * It changes nothing in the model: it knows the line which is shown while the mouse is dragged, and the target which the
 * mouse is on or close to, which the connection will end at. */
class ConnectionDrawing {
  private final ConnectionTarget start;
  private final TargetFinder finder;
  private final Point2D pressedAt;
  private Point2D mouse;
  private ConnectionTarget target;
  private boolean hasLeftStart;

  /** @param start what the connection starts at, framed by the area where it is
   * @param finder finds what the connection may end at */
  ConnectionDrawing(ConnectionTarget start, TargetFinder finder, Point2D pressedAt) {
    this.start = start;
    this.finder = finder;
    this.pressedAt = pressedAt;
    this.mouse = pressedAt;
  }

  /** Follows the mouse. A relation can end at the class it starts at, which makes a loop, only after the mouse has
   * been away from it, so that a shaky click on a class draws none.
   * @param tolerance how close the mouse has to come to a target to end the connection at it */
  void to(Point2D at, double tolerance) {
    mouse = at;
    ConnectionTarget picked = finder.closestTo(at, tolerance);
    boolean onStart = picked != null && picked.element() == start.element();
    hasLeftStart = hasLeftStart || !onStart;
    target = onStart && !hasLeftStart ? null : picked;
  }

  /** @return what the connection starts at */
  ConnectionTarget start() { return start; }

  /** @return what the connection would end at, null if the mouse is on nothing */
  ConnectionTarget target() { return target; }

  /** @return what is framed, the area of the class or attribute which the connection would end at, null if the mouse
   * is on none */
  Rectangle2D targetFrame() { return target != null ? target.frame() : null; }

  /** @return the line of the relation which the connection would end at, null if it would end at none */
  SceneRelationLine targetLine() { return target != null ? target.thickLine() : null; }

  /** @return the line which is shown: from the point where the mouse was pressed to the mouse, wherever it is. The
   * target which the connection would end at is marked by its frame, the line only jumps to its place when the
   * connection is created. */
  Line2D line() {
    return new Line2D.Double(pressedAt, mouse);
  }
}
