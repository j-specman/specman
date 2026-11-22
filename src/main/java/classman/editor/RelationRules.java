package classman.editor;

import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.RelationType_V001;

/** Which boxes a relation may connect, so that the tool for it only starts and ends where the relation makes sense.
 * <ul>
 * <li>An implementation starts at a class or an enum and ends at an interface.</li>
 * <li>An inheritance connects two classes or two interfaces. An enum neither extends nor is extended.</li>
 * <li>An association, aggregation or composition doesn't start at an enum, which doesn't know who uses it. It may end
 * at one: a directed association to an enum says what an attribute of the enum's type says.</li>
 * </ul> */
final class RelationRules {

  private RelationRules() {}

  static boolean canStart(RelationType_V001 type, ClassModel_V001 from) {
    return switch (type) {
      case IMPLEMENTS -> from.kind != ClassKind_V001.INTERFACE;
      default -> from.kind != ClassKind_V001.ENUM;
    };
  }

  static boolean canEnd(RelationType_V001 type, ClassModel_V001 from, ClassModel_V001 to) {
    return switch (type) {
      case IMPLEMENTS -> to.kind == ClassKind_V001.INTERFACE;
      case EXTENDS -> to.kind == from.kind;
      default -> true;
    };
  }
}
