package specman.util;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

/** Keeps the Alt key away from a text component in a popup. The Windows look and feel, and FlatLaf with it, take
 * a press of Alt for the user leaving the menus and close all open popup menus - and a text editor in a popup counts
 * as one. On a keyboard with an AltGr key, that is the Alt key of Windows together with Ctrl, which makes it
 * impossible to type {@code { } [ ] \ | @} in such an editor. The characters themselves come with their own key
 * typed events, so nothing is lost by not delivering the Alt key events. Only active between
 * {@link #install()} and {@link #uninstall()}. */
public class AltKeyShield implements KeyEventDispatcher {
  private final Component shielded;

  public AltKeyShield(Component shielded) {
    this.shielded = shielded;
  }

  public void install() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
  }

  public void uninstall() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
  }

  /** @return true for the Alt key events of the shielded component, which means they are not processed any further */
  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    return e.getKeyCode() == KeyEvent.VK_ALT && e.getComponent() == shielded;
  }
}
