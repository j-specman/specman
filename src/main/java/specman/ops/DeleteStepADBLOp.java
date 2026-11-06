package specman.ops;

import specman.EditException;
import specman.editarea.InteractiveStepFragment;
import specman.undo.UndoableSchrittEntfernt;
import specman.undo.UndoableZweigEntfernt;
import specman.view.*;
import specman.visitor.VanishingSteps;

import javax.swing.*;

import static specman.view.StepRemovalPurpose.Discard;
import static specman.i18n.Messages.i18n;

public class DeleteStepADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public DeleteStepADBLOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    if (aenderungenVerfolgen()
        && !step.getChangeInfo().isAdded()
        && !step.getChangeInfo().isSourceStep()
        && !(step instanceof CatchBereich)) {
      if (!step.getChangeInfo().isDeleted()) {
        if (isDeletionAllowed(step)) {
          step.alsGeloeschtMarkierenUDBL();
        }
        resyncStepnumberStyleADBL();
      }
    }
    else {
      if (step instanceof CaseSchrittView) {
        CaseSchrittView caseSchritt = (CaseSchrittView) step;
        ZweigSchrittSequenzView zweig = caseSchritt.headingToBranch(initiatingFragment);
        if (zweig != null) {
          VanishingSteps vanishing = VanishingSteps.ofSequence(zweig);
          ensureExternalSourceStepsRemovable(vanishing);
          int zweigIndex = caseSchritt.zweigEntfernen(zweig);
          if (zweigIndex >= 0) {
            addEdit(new UndoableZweigEntfernt(zweig, caseSchritt, zweigIndex));
            resolveMoveTies(vanishing);
          }
          return;
        }
      }
      else if (step instanceof CatchBereich) {
        CatchBereich catchBereich = (CatchBereich) step;
        CatchUeberschrift catchHeading = catchBereich.headingFromFragment(initiatingFragment);
        if (catchHeading != null) {
          if (catchHeading.isPrimaryHeading()) {
            deleteCatchSequence(catchHeading.containingCatchSequence());
          }
          else {
            catchHeading.removeOrMarkAsDeletedUDBL();
          }
        }
        return;
      }
      if (isDeletionAllowed(step)) {
        VanishingSteps vanishing = VanishingSteps.ofStep(step);
        step.markStepnumberLinksAsDefect();
        SchrittSequenzView sequenz = step.getParent();
        int schrittIndex = sequenz.schrittEntfernen(step, Discard);
        addEdit(new UndoableSchrittEntfernt(step, sequenz, schrittIndex));
        resolveMoveTies(vanishing);
        resyncStepnumberStyleADBL();
      }
    }
  }

  private void deleteCatchSequence(CatchSchrittSequenzView catchSequence) throws EditException {
    VanishingSteps vanishing = VanishingSteps.ofSequence(catchSequence);
    boolean removed = catchSequence.isRemovedByDeletion();
    if (removed) {
      ensureExternalSourceStepsRemovable(vanishing);
    }
    catchSequence.removeOrMarkAsDeletedUDBL();
    if (removed) {
      resolveMoveTies(vanishing);
    }
  }

  private void ensureExternalSourceStepsRemovable(VanishingSteps vanishing) throws EditException {
    if (!vanishing.allowsRemovalOfExternalSourceSteps(null)) {
      throw new EditException("msg.delete.last.step");
    }
  }

  /** Source and target steps record a move together. When one side vanishes, the move is void: The source steps of
   * vanishing target steps vanish as well, the target steps of vanishing source steps become ordinary steps. */
  private void resolveMoveTies(VanishingSteps vanishing) throws EditException {
    for (QuellSchrittView sourceStep : vanishing.externalSourceSteps()) {
      sourceStep.markStepnumberLinksAsDefect();
      SchrittSequenzView sequence = sourceStep.getParent();
      int index = sequence.schrittEntfernen(sourceStep, Discard);
      addEdit(new UndoableSchrittEntfernt(sourceStep, sequence, index));
    }
    vanishing.externalTargetSteps().forEach(AbstractSchrittView::detachFromSourceStepUDBL);
  }

  private boolean isDeletionAllowed(AbstractSchrittView step) throws EditException {
    if (!step.sequencesAllowDeletion()) {
      throw new EditException("msg.delete.last.step");
    }
    if (step.hasStepnumberLinks()) {
      int dialogResult = showConfirmDialog(
          i18n("msg.delete.referenced.message"),
          i18n("msg.delete.referenced.title"), JOptionPane.OK_CANCEL_OPTION);
      return dialogResult == JOptionPane.OK_OPTION;
    }
    return true;
  }

}
