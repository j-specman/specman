package specman.ops.buttons;

import specman.*;

public class RejectChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  public RejectChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesRejected = 0;
    changesRejected += getIntro().aenderungenVerwerfen();
    getIntro().aenderungsmarkierungenEntfernen(null);
    changesRejected += getHauptSequenz().aenderungenVerwerfen();
    changesRejected += getOutro().aenderungenVerwerfen();
    getOutro().aenderungsmarkierungenEntfernen(null);
    if (changesRejected > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("Das Diagramm enthält keine Änderungen für das aktuelle ChangeSet.");
    }
  }

}
