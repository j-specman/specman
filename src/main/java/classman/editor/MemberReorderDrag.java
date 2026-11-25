package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;
import classman.render.Scene;
import classman.render.SceneTextArea;
import classman.render.TextKind;

import java.awt.geom.Point2D;
import java.util.List;

/** The dragging of the row of a member up or down within its compartment. The order in the model changes as the mouse
 * moves, so the diagram shows the result before the mouse is released, and can be put back. */
class MemberReorderDrag extends Drag {
  private final List<? extends AbstractMemberModel_V001> members;
  private final AbstractMemberModel_V001 member;
  private final List<AbstractMemberModel_V001> orderBefore;
  private final List<Double> otherCentersY;
  private final double pressedY;
  private final double startCenterY;

  /** @param startScene the diagram before the drag */
  MemberReorderDrag(ClassModel_V001 owner, Compartment_V001 compartment, SceneTextArea row, Scene startScene,
                    Point2D pressedAt) {
    this.members = owner.members(compartment);
    this.member = row.member;
    this.orderBefore = List.copyOf(members);
    this.pressedY = pressedAt.getY();
    this.startCenterY = row.area.getCenterY();
    this.otherCentersY = startScene.textAreas.stream()
      .filter(text -> text.element == owner && text.kind == TextKind.lineOf(compartment))
      .filter(text -> text.member != null && text.member != member && MemberOrder.isShown(text.member))
      .map(text -> text.area.getCenterY())
      .toList();
  }

  @Override
  boolean hasMoved() { return !orderBefore.equals(members); }

  /** Puts the member in front of the rows whose centers are below the center of its row, which follows the mouse. The
   * places of the other rows are those from before the drag, so the order doesn't flicker while the rows close up. */
  @Override
  void to(Point2D at, double snapTolerance) {
    double centerY = startCenterY + at.getY() - pressedY;
    int place = (int) otherCentersY.stream().filter(otherY -> otherY < centerY).count();
    MemberOrder.moveToPlace(members, member, place);
  }

  @Override
  void cancel() {
    MemberOrder.restore(members, orderBefore);
  }
}
