package specman.ops.buttons;

import specman.*;

public class RejectChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  public RejectChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesRejected = getHauptSequenz().aenderungenVerwerfen(context);
    if (changesRejected > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("Das Diagramm enthält keine Änderungen.");
    }
  }

}
