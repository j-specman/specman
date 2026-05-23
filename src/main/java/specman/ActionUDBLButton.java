package specman;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Use this type of button to run an atomic undo composition from all the undoable
 * changes performed by the action listeners. */
public class ActionUDBLButton extends JButton {
  public ActionUDBLButton(String text) {
    super(text);
  }

  public ActionUDBLButton() { }

  @Deprecated @Override
  public void addActionListener(ActionListener listener) {
    throw new UnsupportedOperationException("This is a button for atomic operations. Use addActionUDBLListener() instead.");
  }

  public void addActionUDBLListener(ActionListener listener) {
    super.addActionListener(new ActionUDBLListener() {
      @Override
      public void performActionUDBL(ActionEvent e) {
        listener.actionPerformed(e);
      }
    });
  }
}
