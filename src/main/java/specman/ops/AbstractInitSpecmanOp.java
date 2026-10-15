package specman.ops;


import javax.swing.*;
import java.io.File;

import static specman.i18n.Messages.i18n;

public abstract class AbstractInitSpecmanOp extends AbstractSpecmanOp {

  static final Object[] UNSAVED_CHANGES_OPTIONS = {
      i18n("button.save"), i18n("button.discard"), i18n("button.cancel")};

  protected AbstractInitSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public static boolean confirmDiscardUnsavedChanges(String filename, Runnable saveAction) {
    int result = JOptionPane.showOptionDialog(
        null,
        i18n("msg.unsaved.message", filename),
        i18n("msg.unsaved.title"),
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        UNSAVED_CHANGES_OPTIONS,
        UNSAVED_CHANGES_OPTIONS[0]);
    if (result == JOptionPane.CLOSED_OPTION || result == 2) {
      return false;
    }
    if (result == 0) {
      saveAction.run();
    }
    return true;
  }

  protected boolean confirmDiscardUnsavedChanges() {
    if (!hasUnsavedChanges()) {
      return true;
    }
    File current = getDiagrammDatei();
    String filename = current != null ? current.getName() : i18n("msg.unsaved.filename.unknown");
    return confirmDiscardUnsavedChanges(filename, () -> context.diagrammSpeichern(false));
  }

}
