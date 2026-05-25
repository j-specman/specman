package specman.ops;

import specman.Aenderungsart;
import specman.EditException;
import specman.editarea.InteractiveStepFragment;
import specman.undo.UndoableSchrittEntfernt;
import specman.undo.UndoableZweigEntfernt;
import specman.view.*;

import javax.swing.*;

import static specman.Aenderungsart.Hinzugefuegt;
import static specman.view.StepRemovalPurpose.Discard;

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
          int zweigIndex = caseSchritt.zweigEntfernen(context, zweig);
          addEdit(new UndoableZweigEntfernt(context, zweig, caseSchritt, zweigIndex));
          return;
        }
      }
      else if (step instanceof CatchBereich) {
        CatchBereich catchBereich = (CatchBereich) step;
        CatchUeberschrift catchHeading = catchBereich.headingFromFragment(initiatingFragment);
        if (catchHeading != null) {
          if (catchHeading.isPrimaryHeading()) {
            catchHeading.containingCatchSequence().removeOrMarkAsDeletedUDBL();
          }
          else {
            catchHeading.removeOrMarkAsDeletedUDBL();
          }
        }
        return;
      }
      if (isDeletionAllowed(step)) {
        step.markStepnumberLinksAsDefect();
        SchrittSequenzView sequenz = step.getParent();
        int schrittIndex = sequenz.schrittEntfernen(step, Discard);
        addEdit(new UndoableSchrittEntfernt(step, sequenz, schrittIndex));
        resyncStepnumberStyleADBL();
      }
    }
  }

  private boolean isDeletionAllowed(AbstractSchrittView step) throws EditException {
    if (!step.getParent().allowsStepDeletion()) {
      throw new EditException("Der letzte Schritt kann nicht entfernt werden.");
    }
    if (step.hasStepnumberLinks()) {
      int dialogResult = showConfirmDialog(
          "Der zu löschende Schritt wird referenziert. Möchten Sie den Schritt " +
              "wirklich löschen? Die Referenzen werden dann als 'Defekt' markiert.",
          "Verknüpfte Schrittreferenzen", JOptionPane.OK_CANCEL_OPTION);
      return dialogResult == JOptionPane.OK_OPTION;
    }
    return true;
  }

}
