package specman.ops.buttons;

import specman.*;

import specman.undo.UndoableZweigHinzugefuegt;
import specman.view.AbstractSchrittView;
import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;

public class CreateCaseBranchADBLOpButton extends AbstractADBLSpecmanOpButton {

  public CreateCaseBranchADBLOpButton(Specman specman) { super(specman); }

  @Override
  void execute() throws EditException {
    dropWelcomeMessage();
    AbstractSchrittView schritt = getHauptSequenz().findeSchritt(getLastFocusedTextArea());
    if (!(schritt instanceof CaseSchrittView)) {
      fehler("Kein Case-Schritt ausgewählt");
      return;
    }
    CaseSchrittView caseSchritt = (CaseSchrittView) schritt;
    ZweigSchrittSequenzView ausgewaehlterZweig = caseSchritt.headingToBranch(getLastFocusedTextArea());
    if (ausgewaehlterZweig == null) {
      fehler("Kein Zweig ausgewählt");
      return;
    }
    ZweigSchrittSequenzView neuerZweig = caseSchritt.neuenZweigHinzufuegen(ausgewaehlterZweig);
    resyncStepnumberStyleADBL();
    addEdit(new UndoableZweigHinzugefuegt(neuerZweig, caseSchritt));
    schritt.skalieren(getZoomFactor(), 100);
    diagrammAktualisieren(schritt.getFirstEditArea());
  }

}
