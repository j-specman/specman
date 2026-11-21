package classman.model.v001.change;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.TextChangeModel_V001;

import java.util.Objects;

/**
 * Gives a class a new name. While changes are tracked, the name gets the mark was(changeSet, "old name") of the
 * change set, so that the change can be accepted or rejected. The mark keeps the name from before the change set
 * started, however often it is renamed, and goes away if the name is set back to it. If another change set has changed
 * the name before, the change set takes it over: its backup is the name from before this rename. The marks inside a
 * class which a change set has added or removed are ignored in the display, so it is renamed without a mark.
 */
public class ClassRenamer_V001 {

    /** Renames the class without tracking. */
    public void rename(ClassModel_V001 box, String newName) {
        box.name = newName;
    }

    /** Renames the class as a change of the change set. */
    public void rename(ClassModel_V001 box, String newName, String changeSet) {
        if (box.change != null) {
            box.name = newName;
            return;
        }
        TextChangeModel_V001 change = box.nameChange;
        if (change != null && change.changeSet.equals(changeSet)) {
            box.nameChange = Objects.equals(change.backup, newName) ? null : change;
        }
        else if (!box.name.equals(newName)) {
            box.nameChange = new TextChangeModel_V001(changeSet, box.name);
        }
        box.name = newName;
    }
}
