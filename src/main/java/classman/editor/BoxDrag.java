package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.Scene;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** The dragging of one box over the diagram. It changes the position in the model as the mouse moves and can put it
 * back. The other boxes keep their places, see {@link PositionFreezer}. */
class BoxDrag extends Drag {
  private final AbstractBoxModel_V001 box;
  private final Point2D pressedAt;
  private final Rectangle2D startArea;
  private final PositionFreezer freezer;

  /** @param startScene the diagram before the drag */
  BoxDrag(AbstractBoxModel_V001 box, Point2D pressedAt, Scene startScene, PositionFreezer freezer) {
    this.box = box;
    this.pressedAt = pressedAt;
    this.startArea = startScene.elementAreaOf(box).area;
    this.freezer = freezer;
  }

  @Override
  boolean hasMoved() { return freezer.isFrozen(); }

  /** Puts the box where it is when the mouse is at the given point, but not left of or above the top left corner of the
   * diagram, which is at (0, 0). A box doesn't snap to anything. */
  @Override
  void to(Point2D at, double snapTolerance) {
    freezer.freeze();
    box.position = new PositionModel_V001(
      Math.max(0, (int) Math.round(startArea.getX() + at.getX() - pressedAt.getX())),
      Math.max(0, (int) Math.round(startArea.getY() + at.getY() - pressedAt.getY())));
  }

  /** Puts everything back where it was before the first move. */
  @Override
  void cancel() {
    freezer.restore();
  }
}
