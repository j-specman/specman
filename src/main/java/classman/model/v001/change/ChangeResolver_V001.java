package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;

/**
 * Accepts or rejects the change marks of one change set in a whole diagram, see change(...) and was(...) in the
 * grammar. The model is changed in place. Marks of other change sets stay as they are.
 */
public class ChangeResolver_V001 {

    /** Added things stay, removed things go, changed texts keep their new text. */
    public void accept(ClassDiagramModel_V001 model, String changeSet) {
        resolve(model, changeSet, ChangeResolution_V001.ACCEPT);
    }

    /** Added things go, removed things stay, changed texts get their backup back. */
    public void reject(ClassDiagramModel_V001 model, String changeSet) {
        resolve(model, changeSet, ChangeResolution_V001.REJECT);
    }

    private void resolve(ClassDiagramModel_V001 model, String changeSet, ChangeResolution_V001 resolution) {
        VanishingElements_V001 vanishing = new VanishingElements_V001(model, changeSet, resolution);
        new ElementRemover_V001(vanishing).remove(model);
        new ElementMarkResolver_V001(changeSet).resolve(model);
        new TextMarkResolver_V001(changeSet, resolution).resolve(model);
    }
}
