package classman.editor;

import javax.swing.JCheckBoxMenuItem;
import java.awt.event.MouseEvent;

/** A check item which keeps the menu open when the mouse chooses it, so that the user can check or uncheck several
 * in a row. The menu closes when the user clicks somewhere else. */
class StayOpenCheckBoxMenuItem extends JCheckBoxMenuItem {

  StayOpenCheckBoxMenuItem(String text, boolean selected) {
    super(text, selected);
  }

  /** The release of the mouse button is what makes the look and feel close the menu; it is taken here and only
   * switches the item. */
  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_RELEASED && contains(e.getPoint())) {
      if (isEnabled()) {
        doClick(0);
      }
      setArmed(true);
      return;
    }
    super.processMouseEvent(e);
  }
}
