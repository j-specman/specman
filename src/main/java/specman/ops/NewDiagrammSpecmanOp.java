package specman.ops;

public class NewDiagrammSpecmanOp extends AbstractInitSpecmanOp {

  public NewDiagrammSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void create() {
    if (!confirmDiscardUnsavedChanges()) {
      return;
    }
    AutoSaveOp.deleteWorkingCopyFor(getDiagrammDatei());
    clearFocusHistory();
    // An orphaned text area of the previous diagram would swallow insertions
    context().setLastFocusedTextArea(null);
    setChangeModeEnabled(false);
    context().initEmptyDiagram();
    zoomFaktorAnzeigeAktualisieren(100);
  }

}
