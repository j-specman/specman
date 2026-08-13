package specman.ops;

import specman.view.KlappButton;

public class NewDiagrammSpecmanOp extends AbstractInitSpecmanOp {

  public NewDiagrammSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void create() {
    if (!confirmDiscardUnsavedChanges()) {
      return;
    }
    AutoSave.deleteBackupFor(getDiagrammDatei());
    clearFocusHistory();
    setChangeModeEnabled(false);
    context().initEmptyDiagram();
    zoomFaktorAnzeigeAktualisieren(100);
    KlappButton.scaleIcons(100, 0);
  }

}
