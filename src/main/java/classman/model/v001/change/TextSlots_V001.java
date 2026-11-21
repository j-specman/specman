package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.Visibility_V001;

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
            slots.add(textOf(note));
        }
        return slots;
    }

    static TextSlot_V001 nameOf(ClassModel_V001 classModel) {
        return mandatory(() -> classModel.name, text -> classModel.name = text,
            () -> classModel.nameChange, mark -> classModel.nameChange = mark);
    }

    static TextSlot_V001 textOf(NoteModel_V001 note) {
        return mandatory(() -> note.text, text -> note.text = text,
            () -> note.textChange, mark -> note.textChange = mark);
    }

    static TextSlot_V001 labelOf(AbstractRelationModel_V001 relation) {
        return optional(() -> relation.label, text -> relation.label = text,
            () -> relation.labelChange, mark -> relation.labelChange = mark);
    }

    static TextSlot_V001 fromMultiplicityOf(AssociationRelationModel_V001 association) {
        return optional(() -> association.fromMultiplicity, text -> association.fromMultiplicity = text,
            () -> association.fromMultiplicityChange, mark -> association.fromMultiplicityChange = mark);
    }

    static TextSlot_V001 toMultiplicityOf(AssociationRelationModel_V001 association) {
        return optional(() -> association.toMultiplicity, text -> association.toMultiplicity = text,
            () -> association.toMultiplicityChange, mark -> association.toMultiplicityChange = mark);
    }

    private static void addClass(List<TextSlot_V001> slots, ClassModel_V001 classModel) {
        slots.add(nameOf(classModel));
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
        slots.add(visibilityOf(attribute));
        slots.add(nameOf(attribute));
        slots.add(typeOf(attribute));
        for (InfoModel_V001 info : attribute.infos) {
            slots.add(infoOf(info));
        }
    }

    /** The sign of the visibility is the text, none is no text. */
    static TextSlot_V001 visibilityOf(AttributeModel_V001 attribute) {
        return optional(() -> attribute.visibility == null ? null : String.valueOf(attribute.visibility.sign),
            text -> attribute.visibility = Visibility_V001.ofSign(text),
            () -> attribute.visibilityChange, mark -> attribute.visibilityChange = mark);
    }

    static TextSlot_V001 nameOf(AttributeModel_V001 attribute) {
        return mandatory(() -> attribute.name, text -> attribute.name = text,
            () -> attribute.nameChange, mark -> attribute.nameChange = mark);
    }

    static TextSlot_V001 typeOf(AttributeModel_V001 attribute) {
        return optional(() -> attribute.type, text -> attribute.type = text,
            () -> attribute.typeChange, mark -> attribute.typeChange = mark);
    }

    static TextSlot_V001 infoOf(InfoModel_V001 info) {
        return optional(() -> info.text, text -> info.text = text,
            () -> info.textChange, mark -> info.textChange = mark);
    }

    private static void addRelation(List<TextSlot_V001> slots, AbstractRelationModel_V001 relation) {
        slots.add(labelOf(relation));
        if (relation instanceof AssociationRelationModel_V001 association) {
            slots.add(fromMultiplicityOf(association));
            slots.add(toMultiplicityOf(association));
        }
    }
}
