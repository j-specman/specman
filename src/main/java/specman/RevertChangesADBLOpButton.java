package specman;

import javax.swing.*;

class RevertChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  RevertChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesReverted = specman.getHauptSequenz().aenderungenVerwerfen(specman);
    if (changesReverted > 0) {
      specman.diagrammAktualisieren(null);
    } else {
      JOptionPane.showMessageDialog(specman, "Das Diagramm enthält keine Änderungen.");
    }
  }

}
