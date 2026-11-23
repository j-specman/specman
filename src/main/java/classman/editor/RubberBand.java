package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** The rectangle which the user pulls open on empty space to select the boxes in it. It either replaces the selection,
 * or, if the user holds Ctrl, adds the boxes in it to the boxes which were selected when it was started. */
class RubberBand {
  private final Point2D start;
  private final List<AbstractBoxModel_V001> keptBoxes;
  private Point2D end;

  /** A rectangle which replaces the selection. */
  RubberBand(Point2D start) {
    this(start, null);
  }

  /** @param keptBoxes the boxes which stay selected, null if the rectangle replaces the selection */
  RubberBand(Point2D start, List<AbstractBoxModel_V001> keptBoxes) {
    this.start = start;
    this.keptBoxes = keptBoxes;
    this.end = start;
  }

  boolean isAdditive() { return keptBoxes != null; }

  /** @return the boxes which stay selected when the rectangle is finished, empty if it replaces the selection */
  List<AbstractBoxModel_V001> keptBoxes() { return keptBoxes != null ? keptBoxes : List.of(); }

  void to(Point2D point) { end = point; }

  /** @return the area between the point where the band started and the one where the mouse is, empty as long as the
   * mouse hasn't moved */
  Rectangle2D rectangle() {
    Rectangle2D rectangle = new Rectangle2D.Double(start.getX(), start.getY(), 0, 0);
    rectangle.add(end);
    return rectangle;
  }

  boolean hasMoved() { return !start.equals(end); }
}
