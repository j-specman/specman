package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.TextChangeModel_V001;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives an attribute the visibility, name, type and infos of a line which the user has typed, or makes a new attribute
 * of such a line. While changes are tracked, what differs gets the marks of the change set, see
 * {@link TextTracking_V001}, and a new attribute is marked as added. In a class which a change set has added or
 * removed, and in an attribute of that kind, nothing is marked, because the marks would be ignored.
 * <p>
 * A visibility sign which was there can't be removed while changes are tracked: with no sign left there is nothing to
 * carry the mark, see {@link #canApply}.
 */
public class AttributeChanger_V001 {
    private final AttributeInfosChanger_V001 infosChanger = new AttributeInfosChanger_V001();

    /** @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @return false if the change set can't give the attribute the line, because it would have to mark a visibility
     * sign as removed. The sign can go if the change set has added it itself, or if the attribute is added or removed. */
    public boolean canApply(ClassModel_V001 owner, AttributeModel_V001 attribute, AttributeLine_V001 line,
                            String changeSet) {
        if (changeSet == null || !isMarkable(owner, attribute) || line.visibility != null
            || attribute.visibility == null) {
            return true;
        }
        TextChangeModel_V001 mark = attribute.visibilityChange;
        return mark != null && mark.changeSet.equals(changeSet) && TextTracking_V001.isNone(mark.backup);
    }

    /** Gives the attribute the line.
     * @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @throws IllegalArgumentException if {@link #canApply} says no */
    public void apply(ClassModel_V001 owner, AttributeModel_V001 attribute, AttributeLine_V001 line, String changeSet) {
        if (!canApply(owner, attribute, line, changeSet)) {
            throw new IllegalArgumentException("A visibility sign which can't be marked as removed stays");
        }
        boolean markable = isMarkable(owner, attribute);
        TextTracking_V001.set(TextSlots_V001.visibilityOf(attribute),
            line.visibility == null ? "" : String.valueOf(line.visibility.sign), changeSet, markable);
        TextTracking_V001.set(TextSlots_V001.nameOf(attribute), line.name, changeSet, markable);
        TextTracking_V001.set(TextSlots_V001.typeOf(attribute), line.type == null ? "" : line.type, changeSet,
            markable);
        infosChanger.apply(attribute, line.infos, changeSet, markable);
    }

    /** @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @return a new attribute with the line, marked as added by the change set */
    public AttributeModel_V001 create(ClassModel_V001 owner, AttributeLine_V001 line, String changeSet) {
        List<InfoModel_V001> infos = new ArrayList<>();
        for (String info : line.infos) {
            infos.add(new InfoModel_V001(info));
        }
        AttributeModel_V001 attribute = new AttributeModel_V001(line.visibility, line.name, line.type, infos);
        if (changeSet != null && owner.change == null) {
            attribute.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
        }
        return attribute;
    }

    private boolean isMarkable(ClassModel_V001 owner, AttributeModel_V001 attribute) {
        return owner.change == null && attribute.change == null;
    }
}
