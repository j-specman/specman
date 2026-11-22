package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;

/** Removes the elements, attributes and association classes which go away, the survivors keep their order. */
class ElementRemover_V001 {
    private final VanishingElements_V001 vanishing;

    ElementRemover_V001(VanishingElements_V001 vanishing) {
        this.vanishing = vanishing;
    }

    void remove(ClassDiagramModel_V001 model) {
        model.elements.removeIf(element -> elementVanishes(element));
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            if (vanishing.vanishes(classModel.stereotypeElementChange)) {
                StereotypeChanger_V001.clear(classModel);
            }
            classModel.attributes.removeIf(attribute -> vanishing.vanishes(attribute.change));
            for (AttributeModel_V001 attribute : classModel.attributes) {
                attribute.infos.removeIf(info -> vanishing.vanishes(info.change));
            }
        }
        for (AbstractRelationModel_V001 relation : model.relations()) {
            removeVanishingAssocClass(relation);
        }
    }

    private boolean elementVanishes(Object element) {
        if (element instanceof ClassModel_V001 classModel) {
            return vanishing.vanishes(classModel.change);
        }
        if (element instanceof AbstractRelationModel_V001 relation) {
            return vanishing.relationVanishes(relation);
        }
        return vanishing.noteVanishes((NoteModel_V001) element);
    }

    private void removeVanishingAssocClass(AbstractRelationModel_V001 relation) {
        if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null
                && vanishing.vanishes(association.assocClass.change)) {
            association.assocClass = null;
        }
    }
}
