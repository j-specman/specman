package classman.editor;

import org.junit.jupiter.api.Test;

import javax.swing.Icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MirroredIconTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  /** The look and feel makes the icon of a menu entry which can't be chosen from the icon itself. A mirrored icon has to
   * give one, or the entry has no icon at all. */
  @Test
  void disabledIconIsMirroredToo() {
    MirroredIcon icon = new MirroredIcon(EditorIcons.read(EditorIcons.COMPOSITION, 20));

    Icon disabled = icon.getDisabledIcon();

    assertNotNull(disabled);
    assertTrue(disabled instanceof MirroredIcon);
    assertEquals(icon.getIconWidth(), disabled.getIconWidth());
  }
}
