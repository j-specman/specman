package classman.editor;

import java.awt.Dimension;
import java.awt.Rectangle;

/** Where a window opens: as near as possible to the thing it belongs to, but always completely on the screen if it
 * fits there. Being seen completely wins over being near. */
final class WindowPlacement {
  private WindowPlacement() {}

  /** @param size the size the window would like to have
   * @param anchor the area on the screen the window belongs to, which the window is centered on if it can be
   * @param usableScreen the part of the screen without task bars and the like
   * @return the bounds of the window: no larger than the screen and, if there is room, within the screen */
  static Rectangle of(Dimension size, Rectangle anchor, Rectangle usableScreen) {
    int width = Math.min(size.width, usableScreen.width);
    int height = Math.min(size.height, usableScreen.height);
    int x = within(anchor.x + (anchor.width - width) / 2, usableScreen.x, usableScreen.width - width);
    int y = within(anchor.y + (anchor.height - height) / 2, usableScreen.y, usableScreen.height - height);
    return new Rectangle(x, y, width, height);
  }

  private static int within(int wanted, int start, int room) {
    return Math.max(start, Math.min(wanted, start + room));
  }
}
