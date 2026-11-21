package classman.editor;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowPlacementTest {
  private static final Rectangle SCREEN = new Rectangle(0, 40, 1920, 1000);
  private static final Dimension WINDOW = new Dimension(600, 400);

  /** If there is room, the window is centered on the thing it belongs to. */
  @Test
  void centeredOnAnchor() {
    Rectangle bounds = WindowPlacement.of(WINDOW, new Rectangle(800, 400, 200, 100), SCREEN);

    assertEquals(new Rectangle(600, 250, 600, 400), bounds);
  }

  /** A diagram at the screen's edge doesn't pull the window out of the screen. */
  @Test
  void pushedIntoScreen() {
    Rectangle bounds = WindowPlacement.of(WINDOW, new Rectangle(1800, 900, 200, 200), SCREEN);

    assertEquals(new Rectangle(1320, 640, 600, 400), bounds);
  }

  /** An anchor above and left of the usable screen, e.g. partly scrolled away. */
  @Test
  void pushedIntoScreenAtTopLeft() {
    Rectangle bounds = WindowPlacement.of(WINDOW, new Rectangle(-300, -100, 200, 100), SCREEN);

    assertEquals(new Rectangle(0, 40, 600, 400), bounds);
  }

  /** A window larger than the screen shrinks to it. */
  @Test
  void shrunkToScreen() {
    Rectangle bounds = WindowPlacement.of(new Dimension(3000, 2000), new Rectangle(500, 500, 10, 10), SCREEN);

    assertEquals(SCREEN, bounds);
  }

  /** On a second monitor left of the main one, screen coordinates are negative. */
  @Test
  void secondMonitorLeft() {
    Rectangle left = new Rectangle(-1920, 0, 1920, 1080);

    Rectangle bounds = WindowPlacement.of(WINDOW, new Rectangle(-100, 500, 100, 100), left);

    assertEquals(new Rectangle(-600, 350, 600, 400), bounds);
  }
}
