package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.Scene;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** The dragging of the left or right side of a class, which gives it a width of its own. The opposite side stays
 * where it is, so dragging the left side moves the box too. The box doesn't get narrower than its layout allows, which
 * is the widest word of its name. A class which is resized has a position afterwards, see {@link PositionFreezer}. */
class BoxResize extends Drag {
  private final ClassModel_V001 box;
  private final ResizeSide side;
  private final Rectangle2D startArea;
  private final double minimumWidth;
  private final PositionFreezer freezer;
  private final Integer widthBefore;

  /** @param startScene the diagram before the drag
   * @param minimumWidth the width below which the box can't be made, in units of the diagram */
  BoxResize(ClassModel_V001 box, ResizeSide side, Scene startScene, double minimumWidth, PositionFreezer freezer) {
    this.box = box;
    this.side = side;
    this.startArea = startScene.elementAreaOf(box).area;
    this.minimumWidth = minimumWidth;
    this.freezer = freezer;
    this.widthBefore = box.width;
  }

  @Override
  boolean hasMoved() { return freezer.isFrozen(); }

  /** Gives the box the width which puts the dragged side where the mouse is. */
  @Override
  void to(Point2D at, double snapTolerance) {
    freezer.freeze();
    int left = box.position.x;
    int right = (int) Math.round(startArea.getMaxX());
    int width = side == ResizeSide.RIGHT ? (int) Math.round(at.getX() - left) : (int) Math.round(right - at.getX());
    box.width = Math.max(width, (int) Math.ceil(minimumWidth));
    if (side == ResizeSide.LEFT) {
      box.position = new PositionModel_V001(right - box.width, box.position.y);
    }
  }

  /** Puts the width and all positions back as they were before the first move. */
  @Override
  void cancel() {
    freezer.restore();
    box.width = widthBefore;
  }
}
