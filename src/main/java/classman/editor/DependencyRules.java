package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;

/** What a dependency may connect, so that the tool for it only starts and ends where it makes sense. Both ends are
 * classes, interfaces, enums, attributes (the values of an enum included) or methods, none of which a change set has removed.
 * An attribute of a removed class counts as removed. The two ends belong to different classes: a class and its own
 * attributes and methods, or two members of one class, depend on each other by nature. */
final class DependencyRules {

  private DependencyRules() {}

  /** @param member null if the dependency starts or ends at the class itself */
  static boolean canStartAt(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    return isLive(box, member);
  }

  /** @param fromBox the class which the dependency starts at, or which owns the attribute it starts at
   * @param member null if the dependency ends at the class itself */
  static boolean canEndAt(ClassModel_V001 fromBox, ClassModel_V001 box, AbstractMemberModel_V001 member) {
    return box != fromBox && isLive(box, member);
  }

  private static boolean isLive(ClassModel_V001 box, AbstractMemberModel_V001 member) {
    return !isRemoved(box.change) && (member == null
      || !isRemoved(member.change) && !member.hiddenMarker);
  }

  private static boolean isRemoved(ChangeModel_V001 change) {
    return change != null && change.type == ChangeType_V001.REMOVED;
  }
}
