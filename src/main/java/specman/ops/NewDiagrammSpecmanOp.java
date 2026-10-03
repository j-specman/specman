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
    setChangeModeEnabled(false);
    context().initEmptyDiagram();
    zoomFaktorAnzeigeAktualisieren(100);
  }

}
