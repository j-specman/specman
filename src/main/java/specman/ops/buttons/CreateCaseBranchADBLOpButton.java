package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.undo.UndoableZweigHinzugefuegt;
import specman.view.AbstractSchrittView;
import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;


public class CreateCaseBranchADBLOpButton extends AbstractADBLSpecmanOpButton implements DragSourceProvider {

  public CreateCaseBranchADBLOpButton(Specman specman) { super(specman); }

  public DragSource dragSource() { return new DragSource.CaseBranchCreation(); }

  @Override
  void execute() throws EditException {
    dropWelcomeMessage();
    AbstractSchrittView schritt = getHauptSequenz().findeSchritt(getLastFocusedTextArea());
    if (!(schritt instanceof CaseSchrittView)) {
      fehler("msg.case.branch.no.case.step");
      return;
    }
    CaseSchrittView caseSchritt = (CaseSchrittView) schritt;
    ZweigSchrittSequenzView ausgewaehlterZweig = caseSchritt.headingToBranch(getLastFocusedTextArea());
    if (ausgewaehlterZweig == null) {
      fehler("msg.case.branch.no.branch");
      return;
    }
    ZweigSchrittSequenzView neuerZweig = caseSchritt.neuenZweigHinzufuegen(ausgewaehlterZweig);
    resyncStepnumberStyleADBL();
    addEdit(new UndoableZweigHinzugefuegt(neuerZweig, caseSchritt));
    schritt.skalieren(getZoomFactor(), 100);
    diagrammAktualisieren(schritt.getFirstEditArea());
  }

}
