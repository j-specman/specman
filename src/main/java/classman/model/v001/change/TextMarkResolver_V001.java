package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
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
        for (TextSlot_V001 slot : TextSlots_V001.of(model)) {
            if (isMine(slot.mark())) {
                slot.setText(slot.isOptional() ? optional(slot) : mandatory(slot));
                slot.setMark(null);
            }
        }
        removeGoneTexts(model);
    }

    private boolean isMine(TextChangeModel_V001 mark) {
        return mark != null && mark.changeSet.equals(changeSet);
    }

    /** A text which always exists: accepting keeps it, rejecting brings back the backup. */
    private String mandatory(TextSlot_V001 slot) {
        if (resolution == ChangeResolution_V001.ACCEPT || slot.mark().backup == null) {
            return slot.text();
        }
        return slot.mark().backup;
    }

    /** A text which may be absent: null if it is empty or didn't exist before. */
    private String optional(TextSlot_V001 slot) {
        String text = resolution == ChangeResolution_V001.ACCEPT ? slot.text() : slot.mark().backup;
        return text == null || text.isEmpty() ? null : text;
    }

    /** Tagged values and info items without a text are taken out, and a stereotype without a text loses its ID and marks. */
    private void removeGoneTexts(ClassDiagramModel_V001 model) {
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            if (classModel.stereotype == null) {
                StereotypeChanger_V001.clear(classModel);
            }
            classModel.taggedValues.removeIf(tag -> tag.value == null);
            for (AttributeModel_V001 attribute : classModel.attributes) {
                attribute.infos.removeIf(info -> info.text == null);
            }
        }
    }
}
