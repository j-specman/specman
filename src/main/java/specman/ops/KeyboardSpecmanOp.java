package specman.ops;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class KeyboardSpecmanOp extends AbstractSpecmanOp {

  private final Set<Integer> pressedKeys = new HashSet<>();

  public KeyboardSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public boolean isKeyPressed(int keyCode) {
    return pressedKeys.contains(keyCode);
  }

  public void register() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
      trackPressedKeys(e);
      return handleScrollKeys(e);
    });
  }

  /**
   * Maintains a set of currently held-down key codes. This is needed by drag-and-drop logic
   * in {@link specman.draganddrop.DraggingLogic} to detect modifier keys (e.g. Ctrl) that
   * are held during a drag operation, since standard Swing mouse events do not reliably
   * carry key state across all platforms.
   */
  private void trackPressedKeys(KeyEvent e) {
    if (e.getID() == KeyEvent.KEY_PRESSED) {
      pressedKeys.add(e.getKeyCode());
    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
      pressedKeys.remove(e.getKeyCode());
    }
  }

  /**
   * Handles Page Up / Page Down keys for the main diagram scroll pane. Without this,
   * those keys only work when the scroll pane itself has focus. Since focus usually
   * sits inside a text edit area within the diagram, the scroll pane never receives
   * these events through normal Swing focus routing — so we intercept them globally here.
   */
  private boolean handleScrollKeys(KeyEvent e) {
    if (e.getID() != KeyEvent.KEY_PRESSED) return false;
    if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
      scrollBy(+1);
      return true;
    }
    if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
      scrollBy(-1);
      return true;
    }
    return false;
  }

}
