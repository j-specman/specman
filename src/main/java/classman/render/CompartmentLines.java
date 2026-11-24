package classman.render;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines of one compartment of a class box which are shown: the members which aren't hidden, and, if some are
 * hidden and the class has no line "..." of its own, a line ". . ." which follows them, marked with the change set of
 * the first mark of the hidden members. A compartment which is not shown has no lines at all.
 */
class CompartmentLines {
    final Compartment_V001 compartment;
    final boolean isShown;
    final List<AbstractMemberModel_V001> members;
    final List<MarkedText> texts;

    private CompartmentLines(Compartment_V001 compartment, boolean isShown, List<AbstractMemberModel_V001> members,
                             List<MarkedText> texts) {
        this.compartment = compartment;
        this.isShown = isShown;
        this.members = members;
        this.texts = texts;
    }

    static CompartmentLines of(ClassModel_V001 box, Compartment_V001 compartment, MemberTextBuilder textBuilder) {
        if (!compartment.isShownIn(box)) {
            return new CompartmentLines(compartment, false, List.of(), List.of());
        }
        List<AbstractMemberModel_V001> shown = shownMembers(box, compartment);
        List<MarkedText> texts = new ArrayList<>();
        for (AbstractMemberModel_V001 member : shown) {
            texts.add(textBuilder.build(member));
        }
        return new CompartmentLines(compartment, true, shown, texts);
    }

    private static List<AbstractMemberModel_V001> shownMembers(ClassModel_V001 box, Compartment_V001 compartment) {
        List<AbstractMemberModel_V001> shown = new ArrayList<>();
        String hiddenMarkChangeSet = null;
        boolean hasHidden = false;
        boolean hasMarker = false;
        for (AbstractMemberModel_V001 member : box.members(compartment)) {
            if (member.hidden) {
                hasHidden = true;
                hiddenMarkChangeSet = hiddenMarkChangeSet == null ? member.firstChangeSet() : hiddenMarkChangeSet;
            }
            else {
                shown.add(member);
                hasMarker = hasMarker || member.hiddenMarker;
            }
        }
        if (hasHidden && !hasMarker) {
            shown.add(compartment.hiddenMarker(hiddenMarkChangeSet));
        }
        return shown;
    }
}
