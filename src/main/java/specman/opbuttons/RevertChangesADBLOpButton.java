package specman.opbuttons;

import specman.*;

public class RevertChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  public RevertChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesReverted = getHauptSequenz().aenderungenVerwerfen(context);
    if (changesReverted > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("Das Diagramm enthält keine Änderungen.");
    }
  }

}
