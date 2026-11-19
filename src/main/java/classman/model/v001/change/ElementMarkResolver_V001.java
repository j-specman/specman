package classman.model.v001.change;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;

/** Drops the added and removed marks of a change set from the elements which are still there. */
class ElementMarkResolver_V001 {
    private final String changeSet;

    ElementMarkResolver_V001(String changeSet) {
        this.changeSet = changeSet;
    }

    void resolve(ClassDiagramModel_V001 model) {
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            element.change = resolved(element.change);
        }
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            classModel.change = resolved(classModel.change);
            for (AttributeModel_V001 attribute : classModel.attributes) {
                attribute.change = resolved(attribute.change);
            }
        }
    }

    private ChangeModel_V001 resolved(ChangeModel_V001 change) {
        return change != null && change.changeSet.equals(changeSet) ? null : change;
    }
}
