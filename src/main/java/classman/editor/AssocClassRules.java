package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;

/** What an association class link may connect, so that the tool for it only starts and ends where it makes sense.
 * <ul>
 * <li>It starts at a class of the kind class, no interface and no enum, which no change set has removed and which
 * is not the association class of a relation already. To use it for another relation the user deletes its link first.</li>
 * <li>It ends at an association, aggregation or composition, no inheritance, which no change set has removed.</li>
 * <li>A class is not the association class of a relation it connects itself.</li>
 * <li>A relation has at most one association class: a relation which a class is the association class of is no
 * target.</li>
 * </ul> */
final class AssocClassRules {

  private AssocClassRules() {}

  static boolean canBeAssocClass(ClassDiagramModel_V001 model, ClassModel_V001 box) {
    boolean isRemoved = box.change != null && box.change.type == ChangeType_V001.REMOVED;
    return box.kind == ClassKind_V001.CLASS && !isRemoved && model.liveAssocClassLinkOf(box) == null;
  }

  static boolean canTake(ClassDiagramModel_V001 model, AbstractRelationModel_V001 relation, ClassModel_V001 box) {
    if (!(relation instanceof AssociationRelationModel_V001)) {
      return false;
    }
    if (relation.fromId.equals(box.id) || relation.toId.equals(box.id)) {
      return false;
    }
    return model.liveAssocClassLinkTo(relation) == null;
  }
}
