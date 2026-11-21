package classman.render;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** The marks of changes stay visible when the diagram is painted small, everything else shrinks with it. A mark's
 * outline keeps a minimum width, and its filled areas keep a minimum size. */
class VisibleMarksTest {

  static {
    // The fonts touch AWT, which reads this property once. Without it, headless tests running later in the same JVM fail.
    System.setProperty("java.awt.headless", "true");
  }

  private static final int WHITE = Color.WHITE.getRGB();
  private static final double STROKE_WIDTH = 12;
  private static final double SMALL = 0.1;

  /** Box from (100,100) to (300,300), so at SMALL it spans pixel 10 to 30. */
  private static SceneRectangle box() {
    return new SceneRectangle(100, 100, 200, 200, null, "#ff0000", STROKE_WIDTH);
  }

  @Test
  void testMarkFrameStaysVisibleWhenSmall() {
    BufferedImage image = paint(box().keepingVisible(), SMALL);

    assertNotEquals(WHITE, image.getRGB(8, 20), "two pixels outside the border belong to the frame");
  }

  @Test
  void testPlainOutlineShrinksWhenSmall() {
    BufferedImage image = paint(box(), SMALL);

    assertEquals(WHITE, image.getRGB(8, 20), "the outline reaches only 0.6 pixels out at this size");
  }

  @Test
  void testMarkKeepsItsOwnWidthWhenBigEnough() {
    BufferedImage image = paint(box().keepingVisible(), 1);

    assertEquals(WHITE, image.getRGB(92, 200), "the frame reaches 6 pixels out of the border at 100%, no more");
    assertNotEquals(WHITE, image.getRGB(95, 200), "the frame reaches 6 pixels out of the border at 100%");
  }

  @Test
  void testMarkBandOfRelationStaysVisibleWhenSmall() {
    ScenePolyline band = new ScenePolyline(new double[] {100, 300}, new double[] {200, 200}, false, "#ff0000", 9);

    BufferedImage plain = paint(band, SMALL);
    BufferedImage marked = paint(band.keepingVisible(), SMALL);

    assertEquals(WHITE, plain.getRGB(20, 22), "the plain band is 0.9 pixels wide, so 2 pixels off the middle is empty");
    assertNotEquals(WHITE, marked.getRGB(20, 22), "the marked band is 6 pixels wide");
  }

  /** A highlight of 40x14 at (100,100) spans 4x1.4 pixels at SMALL, from pixel 10 to 14 and 10 to 11.4. */
  private static SceneRectangle highlight() {
    return new SceneRectangle(100, 100, 40, 14, "#ff0000", null);
  }

  @Test
  void testHighlightGrowsWhenSmall() {
    BufferedImage image = paint(highlight().keepingVisible(), SMALL);

    assertNotEquals(WHITE, image.getRGB(12, 14), "a marked highlight is at least 8 pixels high, around its center");
    assertNotEquals(WHITE, image.getRGB(9, 11), "a marked highlight is at least 8 pixels wide, around its center");
  }

  @Test
  void testPlainFillShrinksWhenSmall() {
    BufferedImage image = paint(highlight(), SMALL);

    assertEquals(WHITE, image.getRGB(12, 14), "a plain fill covers only its own area");
  }

  @Test
  void testHighlightKeepsItsOwnSizeWhenBigEnough() {
    BufferedImage image = paint(highlight().keepingVisible(), 1);

    assertEquals(WHITE, image.getRGB(120, 98), "at 100% the highlight covers only its own area");
    assertNotEquals(WHITE, image.getRGB(120, 105), "at 100% the highlight covers its own area");
  }

  @Test
  void testMarksOfPolygonAndTranslatedCopiesKeepTheFlag() {
    ScenePolygon note = new ScenePolygon(new double[] {0, 10, 10}, new double[] {0, 0, 10}, null, "#ff0000", 12)
      .keepingVisible();

    assertEquals(true, note.translated(5, 5).keepsVisible);
    assertEquals(true, box().keepingVisible().translated(5, 5).keepsVisible);
  }

  private static BufferedImage paint(AbstractSceneElement element, double scale) {
    int size = (int) Math.ceil(400 * scale) + 1;
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, size, size);
      g.scale(scale, scale);
      new SceneGraphicsPainter().paint(new Scene(List.of(element), 0, 0, 400, 400), g);
    }
    finally {
      g.dispose();
    }
    return image;
  }
}
