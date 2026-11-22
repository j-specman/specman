package classman.editor;

import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StayOpenCheckBoxMenuItemTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private MouseEvent release(StayOpenCheckBoxMenuItem item) {
    return new MouseEvent(item, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 1, 1, 1, false);
  }

  /** Choosing the item with the mouse switches it, and the menu stays on the screen: the selected path of the menu
   * manager, which the look and feel clears to close a menu, is untouched. */
  @Test
  void mouseSwitchesAndKeepsMenuOpen() {
    JMenu menu = new JMenu("m");
    StayOpenCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem("x", true);
    menu.add(item);
    item.setSize(100, 20);
    MenuElement[] path = {new JPopupMenu(), menu, menu.getPopupMenu(), item};
    MenuSelectionManager.defaultManager().setSelectedPath(path);

    item.dispatchEvent(release(item));

    assertFalse(item.isSelected());
    assertEquals(path.length, MenuSelectionManager.defaultManager().getSelectedPath().length);
    item.dispatchEvent(release(item));
    assertTrue(item.isSelected());
    MenuSelectionManager.defaultManager().clearSelectedPath();
  }

  /** A disabled item is not switched. */
  @Test
  void disabledItemStays() {
    StayOpenCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem("x", true);
    item.setSize(100, 20);
    item.setEnabled(false);

    item.dispatchEvent(release(item));

    assertTrue(item.isSelected());
  }
}
