package classman.model.v001.change;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;

/**
 * Gives a class a stereotype, changes it or removes it. A class has at most one, which is a list entry like an info of
 * an attribute. With a change set, a new one is marked as added, a removed one is marked as removed and keeps its
 * text, a changed one gets a text mark with its old text. One which a change set has added is simply gone if it is
 * removed, and one which the change set has removed and which is typed again is not removed any more. The marks
 * inside a class which a change set has added or removed are ignored in the display, so there are none then.
 */
class StereotypeChanger_V001 {

    /** @param newText without the angle brackets, empty to remove the stereotype
     * @param changeSet null if changes are not tracked */
    void set(ClassModel_V001 box, String newText, String changeSet) {
        boolean markable = changeSet != null && box.change == null;
        if (newText.isEmpty()) {
            remove(box, changeSet, markable);
        }
        else {
            put(box, newText, changeSet, markable);
        }
    }

    /** Takes the stereotype away with everything which belongs to it. */
    static void clear(ClassModel_V001 box) {
        box.stereotype = null;
        box.stereotypeId = null;
        box.stereotypeChange = null;
        box.stereotypeElementChange = null;
    }

    private void remove(ClassModel_V001 box, String changeSet, boolean markable) {
        if (box.stereotype == null || isMarked(box, ChangeType_V001.REMOVED)) {
            return;
        }
        if (!markable || isMarked(box, ChangeType_V001.ADDED)) {
            clear(box);
        }
        else {
            box.stereotypeElementChange = new ChangeModel_V001(ChangeType_V001.REMOVED, changeSet);
        }
    }

    private void put(ClassModel_V001 box, String newText, String changeSet, boolean markable) {
        if (box.stereotype == null) {
            box.stereotype = newText;
            if (markable) {
                box.stereotypeElementChange = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
            }
            return;
        }
        if (markable && isMarked(box, ChangeType_V001.REMOVED) && box.stereotypeElementChange.changeSet.equals(changeSet)) {
            box.stereotypeElementChange = null;
        }
        TextTracking_V001.set(TextSlots_V001.stereotypeOf(box), newText, changeSet,
            markable && box.stereotypeElementChange == null);
    }

    private static boolean isMarked(ClassModel_V001 box, ChangeType_V001 type) {
        return box.stereotypeElementChange != null && box.stereotypeElementChange.type == type;
    }
}
