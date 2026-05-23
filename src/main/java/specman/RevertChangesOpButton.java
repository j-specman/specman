package specman;

import javax.swing.*;

class RevertChangesOpButton extends AbstractUDBLSpecmanOpButton {

  RevertChangesOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    int changesReverted = specman.hauptSequenz.aenderungenVerwerfen(specman);
    if (changesReverted > 0) {
      specman.diagrammAktualisieren(null);
    } else {
      JOptionPane.showMessageDialog(specman, "Das Diagramm enthält keine Änderungen.");
    }
  }

}
