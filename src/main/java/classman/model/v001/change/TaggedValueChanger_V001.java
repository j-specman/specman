package classman.model.v001.change;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;

/**
 * Gives a tagged value the key and value which the user has typed, makes a new one, or deletes one. While changes are
 * tracked, a changed key or value gets the mark of the change set, see {@link TextTracking_V001}, a new tagged value is
 * marked as added, and a deleted one is marked as removed and keeps its text. One which a change set has added is
 * simply gone if it is deleted. In a class which a change set has added or removed, and in a tagged value of that
 * kind, nothing is marked, because the marks would be ignored.
 */
public class TaggedValueChanger_V001 {

    /** Gives the tagged value the key and value.
     * @param changeSet the change set which tracks the changes, null if changes are not tracked */
    public void apply(ClassModel_V001 owner, TaggedValueModel_V001 tag, TaggedValueLine_V001 line, String changeSet) {
        boolean markable = owner.change == null && tag.change == null;
        TextTracking_V001.set(TextSlots_V001.keyOf(tag), line.key, changeSet, markable);
        TextTracking_V001.set(TextSlots_V001.valueOf(tag), line.value == null ? "" : line.value, changeSet, markable);
    }

    /** @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @return a new tagged value with the key and value, marked as added by the change set */
    public TaggedValueModel_V001 create(ClassModel_V001 owner, TaggedValueLine_V001 line, String changeSet) {
        TaggedValueModel_V001 tag = new TaggedValueModel_V001(line.key, line.value);
        if (changeSet != null && owner.change == null) {
            tag.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
        }
        return tag;
    }

    /** Deletes the tagged value, or marks it as removed by the change set. One which is removed already stays. */
    public void delete(ClassModel_V001 owner, TaggedValueModel_V001 tag, String changeSet) {
        boolean isAdded = tag.change != null && tag.change.type == ChangeType_V001.ADDED;
        if (changeSet == null || owner.change != null || isAdded) {
            owner.taggedValues.remove(tag);
        }
        else if (tag.change == null) {
            tag.change = new ChangeModel_V001(ChangeType_V001.REMOVED, changeSet);
        }
    }
}
