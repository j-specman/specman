package specman.util;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the shield keeps away from the editor in a popup: the Alt key events, e.g. of AltGr, and nothing else. */
class AltKeyShieldTest {

  static {
    // Components touch AWT, which reads this property once. Without it, headless tests running later in the same JVM fail.
    System.setProperty("java.awt.headless", "true");
  }

  private final JTextArea editor = new JTextArea();
  private final AltKeyShield shield = new AltKeyShield(editor);

  private KeyEvent key(JTextArea source, int id, int keyCode, int modifiers) {
    return new KeyEvent(source, id, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
  }

  /** AltGr is reported as Alt together with the AltGr modifier, and Ctrl before it. */
  @Test
  void altGrPressAndReleaseAreKeptAway() {
    int altGr = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;

    assertTrue(shield.dispatchKeyEvent(key(editor, KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT, altGr)));
    assertTrue(shield.dispatchKeyEvent(key(editor, KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT, altGr)));
  }

  @Test
  void plainAltIsKeptAwayToo() {
    assertTrue(shield.dispatchKeyEvent(key(editor, KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK)));
  }

  /** The text the user types must reach the editor, also while AltGr is held. */
  @Test
  void otherKeysReachTheEditor() {
    assertFalse(shield.dispatchKeyEvent(key(editor, KeyEvent.KEY_PRESSED, KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK)));
    assertFalse(shield.dispatchKeyEvent(key(editor, KeyEvent.KEY_PRESSED, KeyEvent.VK_Q, InputEvent.ALT_GRAPH_DOWN_MASK)));
    assertFalse(shield.dispatchKeyEvent(
      new KeyEvent(editor, KeyEvent.KEY_TYPED, 0, InputEvent.ALT_GRAPH_DOWN_MASK, KeyEvent.VK_UNDEFINED, '@')));
  }

  /** The Alt key keeps working everywhere else, e.g. to reach the menu bar. */
  @Test
  void altOfOtherComponentsIsLeftAlone() {
    assertFalse(shield.dispatchKeyEvent(key(new JTextArea(), KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK)));
  }
}
