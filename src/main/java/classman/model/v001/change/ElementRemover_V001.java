package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;

/** Removes the elements and attributes which go away, the survivors keep their order. */
class ElementRemover_V001 {
    private final VanishingElements_V001 vanishing;

    ElementRemover_V001(VanishingElements_V001 vanishing) {
        this.vanishing = vanishing;
    }

    void remove(ClassDiagramModel_V001 model) {
        model.elements.removeIf(element -> elementVanishes(element));
        for (ClassModel_V001 classModel : model.classes()) {
            if (vanishing.vanishes(classModel.stereotypeElementChange)) {
                StereotypeChanger_V001.clear(classModel);
            }
            classModel.taggedValues.removeIf(tag -> vanishing.vanishes(tag.change));
            classModel.attributes.removeIf(attribute -> vanishing.vanishes(attribute.change));
            for (AttributeModel_V001 attribute : classModel.attributes) {
                attribute.infos.removeIf(info -> vanishing.vanishes(info.change));
            }
        }
    }

    private boolean elementVanishes(Object element) {
        if (element instanceof ClassModel_V001 classModel) {
            return vanishing.vanishes(classModel.change);
        }
        if (element instanceof AbstractRelationModel_V001 relation) {
            return vanishing.relationVanishes(relation);
        }
        if (element instanceof AttachLineModel_V001 line) {
            return vanishing.attachLineVanishes(line);
        }
        return vanishing.noteVanishes((NoteModel_V001) element);
    }
}
