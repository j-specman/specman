package classman.model.v001.change;

import classman.model.v001.TextChangeModel_V001;

import java.util.Objects;

/**
 * Gives a text a new content, with or without the mark which says that a change set changed it. With the mark, the
 * backup is the text from before the change set started, however often it is changed, and the mark goes away if the
 * text is set back to it. If another change set has changed the text before, the change set takes it over: its backup
 * is the text from before this change. The empty text removes an optional text, which then stays as the empty text
 * with its mark.
 */
final class TextTracking_V001 {

    private TextTracking_V001() {
    }

    /** Gives the text the new content, with the mark of the change set if there is one and the text can carry it. */
    static void set(TextSlot_V001 slot, String newText, String changeSet, boolean markable) {
        if (changeSet == null) {
            plain(slot, newText);
        }
        else {
            tracked(slot, newText, changeSet, markable);
        }
    }

    static void plain(TextSlot_V001 slot, String newText) {
        slot.setText(slot.isOptional() && newText.isEmpty() ? null : newText);
    }

    /** @param markable false if the marks of the text are ignored in the display, because the element around it has
     * been added or removed: it is changed without a mark then */
    static void tracked(TextSlot_V001 slot, String newText, String changeSet, boolean markable) {
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

    static boolean isNone(String text) {
        return text == null || text.isEmpty();
    }

    private static boolean isSame(String text, String other) {
        return Objects.equals(isNone(text) ? null : text, isNone(other) ? null : other);
    }
}
