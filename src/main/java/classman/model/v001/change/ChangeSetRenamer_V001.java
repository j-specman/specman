package classman.model.v001.change;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.TextChangeModel_V001;

/** Gives the marks of one change set the name of another, for example when two change sets are merged. */
public class ChangeSetRenamer_V001 {

    /** Renames the change set of all element marks and text marks of a diagram. The model is changed in place. */
    public void rename(ClassDiagramModel_V001 model, String from, String to) {
        ElementMarks_V001.replaceAll(model, mark -> renamed(mark, from, to));
        for (TextSlot_V001 slot : TextSlots_V001.of(model)) {
            slot.setMark(renamed(slot.mark(), from, to));
        }
    }

    private ChangeModel_V001 renamed(ChangeModel_V001 mark, String from, String to) {
        return mark != null && mark.changeSet.equals(from) ? new ChangeModel_V001(mark.type, to) : mark;
    }

    private TextChangeModel_V001 renamed(TextChangeModel_V001 mark, String from, String to) {
        return mark != null && mark.changeSet.equals(from) ? new TextChangeModel_V001(to, mark.backup) : mark;
    }
}
