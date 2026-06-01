package specman.draganddrop;

import specman.EditException;
import specman.EditorI;
import specman.Specman;
import specman.undo.UndoableZweigHinzugefuegt;
import specman.view.AbstractSchrittView;
import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;

public class InsertCaseBranchOperation implements DragOperation {

    private final Specman specman;

    public InsertCaseBranchOperation(Specman specman) {
        this.specman = specman;
    }

    @Override
    public boolean canDrop(DropTarget target) {
        return target.referenceStep() instanceof CaseSchrittView;
    }

    @Override
    public void execute(DropTarget target, EditorI editor) throws EditException {
        CaseSchrittView caseStep = (CaseSchrittView) target.referenceStep();
        ZweigSchrittSequenzView targetedBranch = (ZweigSchrittSequenzView) target.sequence();
        ZweigSchrittSequenzView newBranch = caseStep.neuenZweigHinzufuegen(targetedBranch);
        specman.addEdit(new UndoableZweigHinzugefuegt(newBranch, caseStep));
        AbstractSchrittView step = caseStep;
        step.skalieren(specman.getZoomFactor(), 100);
        specman.diagrammAktualisieren(step.getFirstEditArea());
    }
}
