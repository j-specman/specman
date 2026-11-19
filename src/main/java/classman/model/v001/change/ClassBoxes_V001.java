package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;

import java.util.ArrayList;
import java.util.List;

/** The class boxes of a diagram: the classes and the association classes of the relations. */
final class ClassBoxes_V001 {

    private ClassBoxes_V001() {
    }

    static List<ClassModel_V001> of(ClassDiagramModel_V001 model) {
        List<ClassModel_V001> boxes = new ArrayList<>(model.classes());
        for (AbstractRelationModel_V001 relation : model.relations()) {
            if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
                boxes.add(association.assocClass);
            }
        }
        return boxes;
    }
}
