package specman;

import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.undo.UndoableSchrittEntfernt;
import specman.undo.UndoableZweigEntfernt;
import specman.view.*;

import javax.swing.*;

import static specman.Aenderungsart.Hinzugefuegt;
import static specman.view.StepRemovalPurpose.Discard;

class DeleteStepOpButton extends AbstractUDBLSpecmanOpButton {

  DeleteStepOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (specman.lastFocusedTextArea == null) {
      return;
    }
    AbstractSchrittView step = findStep(specman.lastFocusedTextArea);
    deleteStep(step, specman.lastFocusedTextArea);
  }

  void deleteStep(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) throws EditException {
    if (specman.aenderungenVerfolgen()
        && step.getAenderungsart() != Hinzugefuegt
        && !(step instanceof CatchBereich)) {
      if (step.getAenderungsart() != Aenderungsart.Geloescht) {
        markAsDeleted(specman, step);
        specman.resyncStepnumberStyleUDBL();
      }
    }
    else {
      if (step instanceof CaseSchrittView) {
        CaseSchrittView caseSchritt = (CaseSchrittView) step;
        ZweigSchrittSequenzView zweig = caseSchritt.headingToBranch(initiatingFragment);
        if (zweig != null) {
          int zweigIndex = caseSchritt.zweigEntfernen(specman, zweig);
          specman.addEdit(new UndoableZweigEntfernt(specman, zweig, caseSchritt, zweigIndex));
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
      if (isDeletionAllowed(specman, step)) {
        step.markStepnumberLinksAsDefect();
        SchrittSequenzView sequenz = step.getParent();
        int schrittIndex = sequenz.schrittEntfernen(step, Discard);
        specman.addEdit(new UndoableSchrittEntfernt(step, sequenz, schrittIndex));
        specman.resyncStepnumberStyleUDBL();
      }
    }
  }

  private AbstractSchrittView findStep(TextEditArea initiatingTextArea) throws EditException {
    AbstractSchrittView schritt = specman.findeSchritt(initiatingTextArea);
    if (schritt == null) {
      throw new EditException("Ups - niemandem scheint das Feld zu gehören, in dem steht: " + initiatingTextArea.getText());
    }
    return schritt;
  }

  private void markAsDeleted(EditorI editor, AbstractSchrittView step) throws EditException {
    if (isDeletionAllowed(editor, step)) {
      step.alsGeloeschtMarkierenUDBL();
    }
  }

  boolean isDeletionAllowed(EditorI editor, AbstractSchrittView step) throws EditException {
    if (!step.getParent().allowsStepDeletion()) {
      throw new EditException("Der letzte Schritt kann nicht entfernt werden.");
    }
    if (step.hasStepnumberLinks()) {
      int dialogResult = specman.showConfirmDialog(
          "Der zu löschende Schritt wird referenziert. Möchten Sie den Schritt " +
              "wirklich löschen? Die Referenzen werden dann als 'Defekt' markiert.",
          "Verknüpfte Schrittreferenzen", JOptionPane.OK_CANCEL_OPTION);
      return dialogResult == JOptionPane.OK_OPTION;
    }
    return true;
  }

}
