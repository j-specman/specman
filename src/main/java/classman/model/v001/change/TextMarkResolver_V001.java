package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.TextChangeModel_V001;

/**
 * Resolves the text marks of a change set. Accepting keeps the new text, rejecting brings back the backup. An
 * optional text, which is empty or has no backup after that, is gone: the label, type, stereotype, multiplicity, info
 * item or tagged value disappears.
 */
class TextMarkResolver_V001 {
    private final String changeSet;
    private final ChangeResolution_V001 resolution;

    TextMarkResolver_V001(String changeSet, ChangeResolution_V001 resolution) {
        this.changeSet = changeSet;
        this.resolution = resolution;
    }

    void resolve(ClassDiagramModel_V001 model) {
        if (isMine(model.nameChange)) {
            model.name = mandatory(model.name, model.nameChange);
            model.nameChange = null;
        }
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            resolveClass(classModel);
        }
        for (AbstractRelationModel_V001 relation : model.relations()) {
            resolveRelation(relation);
        }
        for (NoteModel_V001 note : model.notes()) {
            if (isMine(note.textChange)) {
                note.text = mandatory(note.text, note.textChange);
                note.textChange = null;
            }
        }
    }

    // ---- classes ----

    private void resolveClass(ClassModel_V001 classModel) {
        if (isMine(classModel.nameChange)) {
            classModel.name = mandatory(classModel.name, classModel.nameChange);
            classModel.nameChange = null;
        }
        if (isMine(classModel.stereotypeChange)) {
            classModel.stereotype = optional(classModel.stereotype, classModel.stereotypeChange);
            classModel.stereotypeChange = null;
            if (classModel.stereotype == null) {
                classModel.stereotypeId = null;
            }
        }
        for (TaggedValueModel_V001 tag : classModel.taggedValues) {
            resolveTaggedValue(tag);
        }
        classModel.taggedValues.removeIf(tag -> tag.value == null);
        for (AttributeModel_V001 attribute : classModel.attributes) {
            resolveAttribute(attribute);
        }
    }

    private void resolveTaggedValue(TaggedValueModel_V001 tag) {
        if (isMine(tag.keyChange)) {
            tag.key = mandatory(tag.key, tag.keyChange);
            tag.keyChange = null;
        }
        if (isMine(tag.valueChange)) {
            tag.value = optional(tag.value, tag.valueChange);
            tag.valueChange = null;
        }
    }

    private void resolveAttribute(AttributeModel_V001 attribute) {
        if (isMine(attribute.nameChange)) {
            attribute.name = mandatory(attribute.name, attribute.nameChange);
            attribute.nameChange = null;
        }
        if (isMine(attribute.typeChange)) {
            attribute.type = optional(attribute.type, attribute.typeChange);
            attribute.typeChange = null;
        }
        for (InfoModel_V001 info : attribute.infos) {
            if (isMine(info.change)) {
                info.text = optional(info.text, info.change);
                info.change = null;
            }
        }
        attribute.infos.removeIf(info -> info.text == null);
    }

    // ---- relations ----

    private void resolveRelation(AbstractRelationModel_V001 relation) {
        if (isMine(relation.labelChange)) {
            relation.label = optional(relation.label, relation.labelChange);
            relation.labelChange = null;
        }
        if (relation instanceof AssociationRelationModel_V001 association) {
            if (isMine(association.fromMultiplicityChange)) {
                association.fromMultiplicity = optional(association.fromMultiplicity, association.fromMultiplicityChange);
                association.fromMultiplicityChange = null;
            }
            if (isMine(association.toMultiplicityChange)) {
                association.toMultiplicity = optional(association.toMultiplicity, association.toMultiplicityChange);
                association.toMultiplicityChange = null;
            }
        }
    }

    // ---- the text itself ----

    private boolean isMine(TextChangeModel_V001 mark) {
        return mark != null && mark.changeSet.equals(changeSet);
    }

    /** A text which always exists: accepting keeps it, rejecting brings back the backup. */
    private String mandatory(String current, TextChangeModel_V001 mark) {
        if (resolution == ChangeResolution_V001.ACCEPT || mark.backup == null) {
            return current;
        }
        return mark.backup;
    }

    /** A text which may be absent: null if it is empty or didn't exist before. */
    private String optional(String current, TextChangeModel_V001 mark) {
        String text = resolution == ChangeResolution_V001.ACCEPT ? current : mark.backup;
        return text == null || text.isEmpty() ? null : text;
    }
}
