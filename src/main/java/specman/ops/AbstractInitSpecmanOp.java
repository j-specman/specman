package specman.ops;

import javax.swing.*;
import java.io.File;

public abstract class AbstractInitSpecmanOp extends AbstractSpecmanOp {

  protected AbstractInitSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  protected boolean confirmDiscardUnsavedChanges() {
    if (!hasUnsavedChanges()) {
      return true;
    }
    File current = getDiagrammDatei();
    String filename = current != null ? current.getName() : "Unbekannt";
    int result = showConfirmDialog(
        "Änderungen am Dokument '" + filename + "' vor dem Laden speichern?" +
        "\nIhre Änderungen gehen verloren, wenn Sie diese nicht speichern.",
        "Diagramm speichern?", JOptionPane.YES_NO_CANCEL_OPTION);
    if (result == JOptionPane.CANCEL_OPTION) {
      return false;
    }
    if (result == JOptionPane.YES_OPTION) {
      context.diagrammSpeichern(false);
    }
    return true;
  }

}
