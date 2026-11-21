package classman.model.v001.change;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.TextChangeModel_V001;

import java.util.Objects;

/**
 * Gives a class a new name or a note a new text. While changes are tracked, the text gets the mark
 * was(changeSet, "old text") of the change set, so that the change can be accepted or rejected. The mark keeps the text
 * from before the change set started, however often it is changed, and goes away if the text is set back to it. If
 * another change set has changed the text before, the change set takes it over: its backup is the text from before this
 * change. The marks inside a class or note which a change set has added or removed are ignored in the display, so
 * those are changed without a mark.
 */
public class ElementTextChanger_V001 {

    /** Renames the class without tracking. */
    public void setClassName(ClassModel_V001 box, String newName) {
        box.name = newName;
    }

    /** Renames the class as a change of the change set. */
    public void setClassName(ClassModel_V001 box, String newName, String changeSet) {
        if (box.change == null) {
            box.nameChange = markAfter(box.name, box.nameChange, newName, changeSet);
        }
        box.name = newName;
    }

    /** Gives the note the text without tracking. */
    public void setNoteText(NoteModel_V001 note, String newText) {
        note.text = newText;
    }

    /** Gives the note the text as a change of the change set. */
    public void setNoteText(NoteModel_V001 note, String newText, String changeSet) {
        if (note.change == null) {
            note.textChange = markAfter(note.text, note.textChange, newText, changeSet);
        }
        note.text = newText;
    }

    /** @return the mark which the text has when the change set has changed it from the current to the new text */
    private TextChangeModel_V001 markAfter(String currentText, TextChangeModel_V001 existing, String newText,
                                           String changeSet) {
        if (existing != null && existing.changeSet.equals(changeSet)) {
            return Objects.equals(existing.backup, newText) ? null : existing;
        }
        if (!currentText.equals(newText)) {
            return new TextChangeModel_V001(changeSet, currentText);
        }
        return existing;
    }
}
