package classman.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A vector graphic which the host gives for a stereotype, painted on a Graphics2D or as PNG for an SVG. */
class VectorImageTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String RED_SQUARE =
    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\" viewBox=\"0 0 10 10\">"
      + "<rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></svg>";

  @Test
  void paintsIntoGivenArea() {
    BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
    var g = canvas.createGraphics();

    VectorImage.of(RED_SQUARE).paint(g, 10, 10, 20, 20);
    g.dispose();

    assertEquals(0xFFFF0000, canvas.getRGB(20, 20));
    assertEquals(0, canvas.getRGB(5, 5));
    assertEquals(0, canvas.getRGB(35, 35));
  }

  @Test
  void pngDataUriHoldsPictureOfGivenPixelSize() throws Exception {
    String uri = VectorImage.of(RED_SQUARE).pngDataUri(64, 64);

    assertTrue(uri.startsWith("data:image/png;base64,"), uri);
    BufferedImage png = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
      Base64.getDecoder().decode(uri.substring("data:image/png;base64,".length()))));
    assertEquals(64, png.getWidth());
    assertEquals(0xFFFF0000, png.getRGB(32, 32));
  }

  @Test
  void invalidSvgIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> VectorImage.of("not an svg"));
  }
}
