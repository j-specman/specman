package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;

/** Finds out what the IDs which relations and lines refer to belong to. */
class ElementOwners {
  private final ClassDiagramModel_V001 model;

  ElementOwners(ClassDiagramModel_V001 model) {
    this.model = model;
  }

  /** @return the box with the ID, or the class which has the attribute with the ID, null if there is none */
  AbstractBoxModel_V001 boxOf(String id) {
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

  /** @return the relation with the ID, null if there is none */
  AbstractRelationModel_V001 relationWithId(String id) {
    return model.relations().stream().filter(relation -> id.equals(relation.id)).findFirst().orElse(null);
  }
}
