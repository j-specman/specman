package classman.model.v001.change;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.TextChangeModel_V001;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives an attribute or a method the visibility, name, type and infos of a line which the user has typed, or makes a
 * new member of such a line. While changes are tracked, what differs gets the marks of the change set, see
 * {@link TextTracking_V001}, and a new member is marked as added. In a class which a change set has added or removed,
 * and in a member of that kind, nothing is marked, because the marks would be ignored.
 * <p>
 * A visibility sign which was there can't be removed while changes are tracked: with no sign left there is nothing to
 * carry the mark, see {@link #canApply}.
 *
 * @param <M> the kind of member
 * @param <L> the line of the kind of member
 */
public abstract class AbstractMemberChanger_V001<M extends AbstractMemberModel_V001, L extends MemberLine_V001> {
    protected final InfoListChanger_V001 entriesChanger = new InfoListChanger_V001();

    /** @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @return false if the change set can't give the member the line, because it would have to mark a visibility
     * sign as removed. The sign can go if the change set has added it itself, or if the member is added or removed. */
    public boolean canApply(ClassModel_V001 owner, M member, L line, String changeSet) {
        if (changeSet == null || !isMarkable(owner, member) || line.visibility != null || member.visibility == null) {
            return true;
        }
        TextChangeModel_V001 mark = member.visibilityChange;
        return mark != null && mark.changeSet.equals(changeSet) && TextTracking_V001.isNone(mark.backup);
    }

    /** Gives the member the line.
     * @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @throws IllegalArgumentException if {@link #canApply} says no */
    public void apply(ClassModel_V001 owner, M member, L line, String changeSet) {
        if (!canApply(owner, member, line, changeSet)) {
            throw new IllegalArgumentException("A visibility sign which can't be marked as removed stays");
        }
        boolean markable = isMarkable(owner, member);
        TextTracking_V001.set(TextSlots_V001.visibilityOf(member),
            line.visibility == null ? "" : String.valueOf(line.visibility.sign), changeSet, markable);
        TextTracking_V001.set(TextSlots_V001.nameOf(member), line.name, changeSet, markable);
        applyOwnParts(member, line, changeSet, markable);
        TextTracking_V001.set(TextSlots_V001.typeOf(member), line.type == null ? "" : line.type, changeSet, markable);
        entriesChanger.apply(member.infos, line.infos, changeSet, markable);
    }

    /** Changes what only this kind of member has, between the name and the type of the line. */
    protected void applyOwnParts(M member, L line, String changeSet, boolean markable) {
    }

    /** @param changeSet the change set which tracks the changes, null if changes are not tracked
     * @return a new member with the line, marked as added by the change set */
    public M create(ClassModel_V001 owner, L line, String changeSet) {
        M member = newMember(line);
        if (changeSet != null && owner.change == null) {
            member.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
        }
        return member;
    }

    /** @return a member with the line and no marks */
    protected abstract M newMember(L line);

    protected static List<InfoModel_V001> entriesOf(List<String> texts) {
        List<InfoModel_V001> entries = new ArrayList<>();
        for (String text : texts) {
            entries.add(new InfoModel_V001(text));
        }
        return entries;
    }

    private static boolean isMarkable(ClassModel_V001 owner, AbstractMemberModel_V001 member) {
        return owner.change == null && member.change == null;
    }
}
