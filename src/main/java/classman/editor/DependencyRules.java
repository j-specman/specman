package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;

/** What a dependency may connect, so that the tool for it only starts and ends where it makes sense. Both ends are
 * classes, interfaces, enums or attributes (the values of an enum included), none of which a change set has removed.
 * An attribute of a removed class counts as removed. The two ends belong to different classes: a class and its own
 * attributes, or two attributes of one class, depend on each other by nature. */
final class DependencyRules {

  private DependencyRules() {}

  /** @param attribute null if the dependency starts or ends at the class itself */
  static boolean canStartAt(ClassModel_V001 box, AttributeModel_V001 attribute) {
    return isLive(box, attribute);
  }

  /** @param fromBox the class which the dependency starts at, or which owns the attribute it starts at
   * @param attribute null if the dependency ends at the class itself */
  static boolean canEndAt(ClassModel_V001 fromBox, ClassModel_V001 box, AttributeModel_V001 attribute) {
    return box != fromBox && isLive(box, attribute);
  }

  private static boolean isLive(ClassModel_V001 box, AttributeModel_V001 attribute) {
    return !isRemoved(box.change) && (attribute == null
      || !isRemoved(attribute.change) && !attribute.hiddenAttributesMarker);
  }

  private static boolean isRemoved(ChangeModel_V001 change) {
    return change != null && change.type == ChangeType_V001.REMOVED;
  }
}
