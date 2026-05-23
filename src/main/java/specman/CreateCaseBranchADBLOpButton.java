package specman;

import specman.undo.UndoableZweigHinzugefuegt;
import specman.view.AbstractSchrittView;
import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;

class CreateCaseBranchADBLOpButton extends AbstractADBLSpecmanOpButton {

  CreateCaseBranchADBLOpButton(Specman specman) { super(specman); }

  @Override
  void execute() throws EditException {
    specman.dropWelcomeMessage();
    AbstractSchrittView schritt = specman.hauptSequenz.findeSchritt(specman.lastFocusedTextArea);
    if (!(schritt instanceof CaseSchrittView)) {
      specman.fehler("Kein Case-Schritt ausgewählt");
      return;
    }
    CaseSchrittView caseSchritt = (CaseSchrittView) schritt;
    ZweigSchrittSequenzView ausgewaehlterZweig = caseSchritt.headingToBranch(specman.lastFocusedTextArea);
    if (ausgewaehlterZweig == null) {
      specman.fehler("Kein Zweig ausgewählt");
      return;
    }
    ZweigSchrittSequenzView neuerZweig = caseSchritt.neuenZweigHinzufuegen(specman, ausgewaehlterZweig);
    specman.resyncStepnumberStyleADBL();
    specman.addEdit(new UndoableZweigHinzugefuegt(specman, neuerZweig, caseSchritt));
    schritt.skalieren(specman.zoomFaktor, 100);
    specman.diagrammAktualisieren(schritt.getFirstEditArea());
  }

}
