package specman.draganddrop;

import specman.EditException;
import specman.EditorI;
import specman.ScrollPause;
import specman.Specman;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.undo.UndoableSchrittVerschoben;
import specman.undo.UndoableSchrittVerschobenMarkiert;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.view.QuellSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.Before;
import static specman.view.StepRemovalPurpose.Move;

public class MoveStepOperation implements DragOperation {

    private final StepnumberLabel label;
    private final AbstractSchrittView movingStep;
    private final Specman specman;

    public MoveStepOperation(StepnumberLabel label, AbstractSchrittView movingStep, Specman specman) {
        this.label = label;
        this.movingStep = movingStep;
        this.specman = specman;
    }

    @Override
    public boolean canDrop(DropTarget target) {
        return target.referenceStep() != null && target.referenceStep() != movingStep;
    }

    @Override
    public void execute(DropTarget target, EditorI editor) throws EditException {
        try (UndoRecording ur = specman.composeUndo(); ScrollPause sp = specman.pauseScrolling()) {
            AbstractSchrittView referenceStep = target.referenceStep();
            SchrittSequenzView targetSequence = referenceStep.getParent();

            if (specman.aenderungenVerfolgen() && !movingStep.getChangeInfo().isAdded()) {
                SchrittSequenzView sourceSequence = movingStep.getParent();
                QuellSchrittView quellschritt;
                if (movingStep.getQuellschritt() == null) {
                    quellschritt = new QuellSchrittView(sourceSequence, movingStep.getId());
                    sourceSequence.insertStep(quellschritt, Before, movingStep);
                }
                else {
                    quellschritt = movingStep.getQuellschritt();
                }
                SchrittSequenzView originalParent = movingStep.getParent();
                int originalIndex = originalParent.schrittEntfernen(movingStep, Move);
                movingStep.setId(referenceStep.newStepIDInSameSequence(target.position()));
                movingStep.setParent(targetSequence);
                targetSequence.insertStep(movingStep, target.position(), referenceStep);
                specman.addEdit(new UndoableSchrittVerschobenMarkiert(movingStep, originalParent, originalIndex, quellschritt));
                movingStep.setQuellschrittUDBL(quellschritt);
                movingStep.setZielschrittStilUDBL();
                quellschritt.setZielschrittUDBL(movingStep);
                specman.getHauptSequenz().resyncStepnumberStyleADBL();
            }
            else {
                SchrittSequenzView originalParent = movingStep.getParent();
                int originalIndex = originalParent.schrittEntfernen(movingStep, Move);
                movingStep.setId(referenceStep.newStepIDInSameSequence(target.position()));
                movingStep.setParent(targetSequence);
                targetSequence.insertStep(movingStep, target.position(), referenceStep);
                specman.addEdit(new UndoableSchrittVerschoben(movingStep, originalParent, originalIndex));
            }
        }
    }
}
