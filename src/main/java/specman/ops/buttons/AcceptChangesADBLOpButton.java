package specman.ops.buttons;

import specman.*;

public class AcceptChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  public AcceptChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesMade = 0;
    changesMade += getIntro().aenderungenUebernehmen();
    getIntro().aenderungsmarkierungenEntfernen(null);
    changesMade += getHauptSequenz().aenderungenUebernehmen(context);
    changesMade += getOutro().aenderungenUebernehmen();
    getOutro().aenderungsmarkierungenEntfernen(null);
    if (changesMade > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("Das Diagramm enthält keine Änderungen.");
    }
  }

}
