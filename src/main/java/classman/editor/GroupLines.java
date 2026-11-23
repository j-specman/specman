package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.PositionModel_V001;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Finds the lines which move with a group of boxes: the relations, anchor lines and association class links whose
 * ends all lie in the group. Their waypoints are absolute points, so they have to be moved with the boxes. Lines with an
 * end outside of the group keep their waypoints, like the lines of a single box which is moved. */
class GroupLines {
  private final ClassDiagramModel_V001 model;
  private final Collection<AbstractBoxModel_V001> members;

  GroupLines(ClassDiagramModel_V001 model, Collection<AbstractBoxModel_V001> members) {
    this.model = model;
    this.members = members;
  }

  /** @return the lists of waypoints, as they are in the model, of the lines which move with the group. Lines without
   * waypoints are not in it. */
  List<List<PositionModel_V001>> waypointLists() {
    List<List<PositionModel_V001>> lists = new ArrayList<>();
    for (AbstractRelationModel_V001 relation : model.relations()) {
      if (relation.route != null && !relation.route.waypoints.isEmpty() && movesWithGroup(relation)) {
        lists.add(relation.route.waypoints);
      }
    }
    for (AttachLineModel_V001 attachLine : model.attachLines()) {
      if (!attachLine.waypoints.isEmpty() && movesWithGroup(attachLine)) {
        lists.add(attachLine.waypoints);
      }
    }
    return lists;
  }

  private boolean movesWithGroup(AbstractRelationModel_V001 relation) {
    return isInGroup(boxOf(relation.fromId)) && isInGroup(boxOf(relation.toId));
  }

  /** An attach line leads from a box to a class, attribute or relation, which moves if its ends do. */
  private boolean movesWithGroup(AttachLineModel_V001 attachLine) {
    if (!isInGroup(boxOf(attachLine.startId))) {
      return false;
    }
    AbstractRelationModel_V001 targetRelation = relationWithId(attachLine.targetId);
    return targetRelation != null ? movesWithGroup(targetRelation) : isInGroup(boxOf(attachLine.targetId));
  }

  private boolean isInGroup(AbstractBoxModel_V001 box) {
    return box != null && members.contains(box);
  }

  /** @return the box with the ID, or the class which has the attribute with the ID, null if there is none */
  private AbstractBoxModel_V001 boxOf(String id) {
    AbstractBoxModel_V001 box = model.boxWithId(id);
    if (box != null) {
      return box;
    }
    for (ClassModel_V001 classModel : model.classes()) {
      for (AttributeModel_V001 attribute : classModel.attributes) {
        if (id.equals(attribute.id)) {
          return classModel;
        }
      }
    }
    return null;
  }

  private AbstractRelationModel_V001 relationWithId(String id) {
    return model.relations().stream().filter(relation -> id.equals(relation.id)).findFirst().orElse(null);
  }
}
