package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.util.ArrayList;
import java.util.List;

import static classman.model.v001.change.TextSlot_V001.mandatory;
import static classman.model.v001.change.TextSlot_V001.optional;

/** Knows every text of a diagram which a change set can change, so that nobody else has to. */
final class TextSlots_V001 {

    private TextSlots_V001() {
    }

    static List<TextSlot_V001> of(ClassDiagramModel_V001 model) {
        List<TextSlot_V001> slots = new ArrayList<>();
        slots.add(mandatory(() -> model.name, text -> model.name = text,
            () -> model.nameChange, mark -> model.nameChange = mark));
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            addClass(slots, classModel);
        }
        for (AbstractRelationModel_V001 relation : model.relations()) {
            addRelation(slots, relation);
        }
        for (NoteModel_V001 note : model.notes()) {
            slots.add(mandatory(() -> note.text, text -> note.text = text,
                () -> note.textChange, mark -> note.textChange = mark));
        }
        return slots;
    }

    private static void addClass(List<TextSlot_V001> slots, ClassModel_V001 classModel) {
        slots.add(mandatory(() -> classModel.name, text -> classModel.name = text,
            () -> classModel.nameChange, mark -> classModel.nameChange = mark));
        slots.add(optional(() -> classModel.stereotype, text -> classModel.stereotype = text,
            () -> classModel.stereotypeChange, mark -> classModel.stereotypeChange = mark));
        for (TaggedValueModel_V001 tag : classModel.taggedValues) {
            slots.add(mandatory(() -> tag.key, text -> tag.key = text,
                () -> tag.keyChange, mark -> tag.keyChange = mark));
            slots.add(optional(() -> tag.value, text -> tag.value = text,
                () -> tag.valueChange, mark -> tag.valueChange = mark));
        }
        for (AttributeModel_V001 attribute : classModel.attributes) {
            addAttribute(slots, attribute);
        }
    }

    private static void addAttribute(List<TextSlot_V001> slots, AttributeModel_V001 attribute) {
        slots.add(mandatory(() -> attribute.name, text -> attribute.name = text,
            () -> attribute.nameChange, mark -> attribute.nameChange = mark));
        slots.add(optional(() -> attribute.type, text -> attribute.type = text,
            () -> attribute.typeChange, mark -> attribute.typeChange = mark));
        for (InfoModel_V001 info : attribute.infos) {
            slots.add(optional(() -> info.text, text -> info.text = text,
                () -> info.change, mark -> info.change = mark));
        }
    }

    private static void addRelation(List<TextSlot_V001> slots, AbstractRelationModel_V001 relation) {
        slots.add(optional(() -> relation.label, text -> relation.label = text,
            () -> relation.labelChange, mark -> relation.labelChange = mark));
        if (relation instanceof AssociationRelationModel_V001 association) {
            slots.add(optional(() -> association.fromMultiplicity, text -> association.fromMultiplicity = text,
                () -> association.fromMultiplicityChange, mark -> association.fromMultiplicityChange = mark));
            slots.add(optional(() -> association.toMultiplicity, text -> association.toMultiplicity = text,
                () -> association.toMultiplicityChange, mark -> association.toMultiplicityChange = mark));
        }
    }
}
