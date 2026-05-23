package specman;

import javax.swing.*;

class AcceptChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  AcceptChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesMade = 0;
    changesMade += specman.intro.aenderungenUebernehmen();
    specman.intro.aenderungsmarkierungenEntfernen(null);
    changesMade += specman.hauptSequenz.aenderungenUebernehmen(specman);
    changesMade += specman.outro.aenderungenUebernehmen();
    specman.outro.aenderungsmarkierungenEntfernen(null);
    if (changesMade > 0) {
      specman.diagrammAktualisieren(null);
    } else {
      JOptionPane.showMessageDialog(specman, "Das Diagramm enthält keine Änderungen.");
    }
  }

}
