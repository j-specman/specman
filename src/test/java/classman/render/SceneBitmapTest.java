package classman.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneBitmapTest {
  static {
    // The fixed-width fonts of the test layout still touch AWT, which reads this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final int WHITE = 0xFFFFFF;

  /** A zoom of 150 percent gives the picture of the SVG frame in one and a half pixels per unit. */
  @Test
  void sizeFollowsZoom() throws Exception {
    Scene scene = SceneTestHelper.layout("class Customer (11111111)");
    SvgFrame frame = SvgFrame.of(scene);

    BufferedImage image = SceneBitmap.of(scene, 1.5);

    assertEquals(Math.round(frame.width * 1.5), image.getWidth());
    assertEquals(Math.round(frame.height * 1.5), image.getHeight());
  }

  /** The margin of the frame is white, and the diagram is painted inside of it. */
  @Test
  void diagramOnWhite() throws Exception {
    BufferedImage image = SceneBitmap.of(SceneTestHelper.layout("class Customer (11111111)"), 2);

    assertEquals(WHITE, image.getRGB(0, 0) & WHITE);
    assertTrue(hasNonWhitePixel(image));
  }

  private static boolean hasNonWhitePixel(BufferedImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) & WHITE) != WHITE) {
          return true;
        }
      }
    }
    return false;
  }
}
