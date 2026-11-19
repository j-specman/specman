package classman.model.v001.change;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;

/** Drops the added and removed marks of a change set from the elements which are still there. */
class ElementMarkResolver_V001 {
    private final String changeSet;

    ElementMarkResolver_V001(String changeSet) {
        this.changeSet = changeSet;
    }

    void resolve(ClassDiagramModel_V001 model) {
        ElementMarks_V001.replaceAll(model, this::resolved);
    }

    private ChangeModel_V001 resolved(ChangeModel_V001 change) {
        return change != null && change.changeSet.equals(changeSet) ? null : change;
    }
}
