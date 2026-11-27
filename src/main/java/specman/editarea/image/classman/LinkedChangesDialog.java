package specman.editarea.image.classman;

import specman.i18n.ICheckBox;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import java.awt.Component;

import static specman.i18n.Messages.i18n;

/** Tells the user that an operation of change mode would change linked diagrams and their files, and lets the user
 * continue with or without changing them, or cancel. */
public class LinkedChangesDialog {

  private LinkedChangesDialog() {
  }

  /** @param count the number of linked diagrams with marks of the change set
   * @return what the user decides, which is {@link LinkedChangesAnswer#CANCEL} if the dialog is closed */
  public static LinkedChangesAnswer ask(Component parent, int count, String changeSetName) {
    JCheckBox changeLinked = new ICheckBox("msg.changes.linked.include", true);
    Object[] buttons = {i18n("button.continue"), i18n("button.cancel")};
    int choice = JOptionPane.showOptionDialog(parent,
      new Object[] {i18n("msg.changes.linked.message", count, changeSetName), changeLinked},
      i18n("msg.changes.linked.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons, buttons[0]);
    if (choice != 0) {
      return LinkedChangesAnswer.CANCEL;
    }
    return changeLinked.isSelected() ? LinkedChangesAnswer.CHANGE_LINKED : LinkedChangesAnswer.LEAVE_LINKED;
  }
}
