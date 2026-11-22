package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.ClassModel_V001;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

/** A connection which the user is drawing from a box to a class, before it is created: a relation from a class, an
 * anchor from a note. It changes nothing in the model: it knows the line which is shown while the mouse is dragged,
 * and the class which the mouse is on or close to, which the connection will end at. */
class ConnectionDrawing {
  private final AbstractBoxModel_V001 from;
  private final Rectangle2D fromArea;
  private final Map<ClassModel_V001, Rectangle2D> classes;
  private Point2D mouse;
  private ClassModel_V001 target;
  private boolean hasLeftStart;

  /** @param fromArea where the box is which the connection starts at
   * @param classes the classes which the connection may end at with their areas, in the order of drawing */
  ConnectionDrawing(AbstractBoxModel_V001 from, Rectangle2D fromArea, Map<ClassModel_V001, Rectangle2D> classes,
                    Point2D pressedAt) {
    this.from = from;
    this.fromArea = fromArea;
    this.classes = classes;
    this.mouse = pressedAt;
  }

  /** Follows the mouse. A relation can end at the class it starts at, which makes a loop, only after the mouse has
   * been away from it, so that a shaky click on a class draws none.
   * @param tolerance how close the mouse has to come to a class to end the connection at it */
  void to(Point2D at, double tolerance) {
    mouse = at;
    ClassModel_V001 picked = ClassPicker.closest(classes, at, tolerance);
    hasLeftStart = hasLeftStart || picked != from;
    target = picked == from && !hasLeftStart ? null : picked;
  }

  AbstractBoxModel_V001 from() { return from; }

  /** @return the class the connection would end at, null if the mouse is on none */
  ClassModel_V001 target() { return target; }

  /** @return where the class is which the connection would end at, null if the mouse is on none */
  Rectangle2D targetArea() { return target != null ? classes.get(target) : null; }

  /** @return the line which is shown: from the center of the first box to the center of the class which the
   * connection would end at, or to the mouse if it is on none */
  Line2D line() {
    Point2D end = target != null
      ? new Point2D.Double(targetArea().getCenterX(), targetArea().getCenterY())
      : mouse;
    return new Line2D.Double(fromArea.getCenterX(), fromArea.getCenterY(), end.getX(), end.getY());
  }
}
