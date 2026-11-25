package classman.editor;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A button which shows a color of a style shows a transparent one on white. */
class ColorSwatchIconTest {

  private static int centerPixelOf(String color) {
    ColorSwatchIcon icon = new ColorSwatchIcon(color);
    BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    icon.paintIcon(null, g, 0, 0);
    g.dispose();
    return image.getRGB(icon.getIconWidth() / 2, icon.getIconHeight() / 2);
  }

  @Test
  void opaqueColorIsShownAsIs() {
    assertEquals(0xff0000, centerPixelOf("#ff0000") & 0xffffff);
  }

  @Test
  void transparentColorIsShownOnWhite() {
    int pixel = centerPixelOf("#ff000080");

    assertEquals(0xff, (pixel >> 16) & 0xff);
    int green = (pixel >> 8) & 0xff;
    assertTrue(green > 100 && green < 160, "pink, green was " + green);
  }
}
