package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.TextChangeModel_V001;

import java.util.Objects;

/**
 * Gives a class a new name, a note a new text, a relation a new label or multiplicity. While changes are tracked, the
 * text gets the mark was(changeSet, "old text") of the change set, so that the change can be accepted or rejected.
 * The mark keeps the text from before the change set started, however often it is changed, and goes away if the text
 * is set back to it. If another change set has changed the text before, the change set takes it over: its backup is
 * the text from before this change. The marks inside an element which a change set has added or removed are ignored
 * in the display, so those are changed without a mark.
 * <p>
 * A label or multiplicity is optional: the empty text removes it. A removed one stays as the empty text with the
 * mark.
 */
public class ElementTextChanger_V001 {

    /** Renames the class without tracking. */
    public void setClassName(ClassModel_V001 box, String newName) {
        plain(TextSlots_V001.nameOf(box), newName);
    }

    /** Renames the class as a change of the change set. */
    public void setClassName(ClassModel_V001 box, String newName, String changeSet) {
        tracked(TextSlots_V001.nameOf(box), newName, changeSet, box.change == null);
    }

    /** Gives the note the text without tracking. */
    public void setNoteText(NoteModel_V001 note, String newText) {
        plain(TextSlots_V001.textOf(note), newText);
    }

    /** Gives the note the text as a change of the change set. */
    public void setNoteText(NoteModel_V001 note, String newText, String changeSet) {
        tracked(TextSlots_V001.textOf(note), newText, changeSet, note.change == null);
    }

    /** Gives the relation the label without tracking. The empty text removes the label. */
    public void setLabel(AbstractRelationModel_V001 relation, String newLabel) {
        plain(TextSlots_V001.labelOf(relation), newLabel);
    }

    /** Gives the relation the label as a change of the change set. */
    public void setLabel(AbstractRelationModel_V001 relation, String newLabel, String changeSet) {
        tracked(TextSlots_V001.labelOf(relation), newLabel, changeSet, relation.change == null);
    }

    /** Gives the end of the relation the multiplicity without tracking. The empty text removes the multiplicity. */
    public void setMultiplicity(AssociationRelationModel_V001 association, RelationEnd_V001 end, String newMultiplicity) {
        plain(multiplicityOf(association, end), newMultiplicity);
    }

    /** Gives the end of the relation the multiplicity as a change of the change set. */
    public void setMultiplicity(AssociationRelationModel_V001 association, RelationEnd_V001 end,
                                String newMultiplicity, String changeSet) {
        tracked(multiplicityOf(association, end), newMultiplicity, changeSet, association.change == null);
    }

    private static TextSlot_V001 multiplicityOf(AssociationRelationModel_V001 association, RelationEnd_V001 end) {
        return end == RelationEnd_V001.FROM
            ? TextSlots_V001.fromMultiplicityOf(association)
            : TextSlots_V001.toMultiplicityOf(association);
    }

    private static void plain(TextSlot_V001 slot, String newText) {
        slot.setText(slot.isOptional() && newText.isEmpty() ? null : newText);
    }

    private static void tracked(TextSlot_V001 slot, String newText, String changeSet, boolean markable) {
        if (!markable) {
            plain(slot, newText);
            return;
        }
        TextChangeModel_V001 mark = slot.mark();
        if (mark != null && mark.changeSet.equals(changeSet)) {
            if (isSame(mark.backup, newText)) {
                slot.setMark(null);
                slot.setText(isNone(mark.backup) && slot.isOptional() ? null : mark.backup);
            }
            else {
                slot.setText(newText);
            }
            return;
        }
        String before = slot.text();
        if (!isSame(before, newText)) {
            slot.setMark(new TextChangeModel_V001(changeSet, before));
            slot.setText(newText);
        }
    }

    private static boolean isSame(String text, String other) {
        return Objects.equals(isNone(text) ? null : text, isNone(other) ? null : other);
    }

    private static boolean isNone(String text) {
        return text == null || text.isEmpty();
    }
}
