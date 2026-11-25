package classman.render;

import classman.model.v001.FontFamily_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The measures depend on the fonts of the machine, so these tests only check what holds for every font. */
class AwtTextMeasurerTest {

  static {
    // The fonts touch AWT, which reads this property once. Without it, headless tests running later in the same JVM fail.
    System.setProperty("java.awt.headless", "true");
  }

  private final AwtTextMeasurer measurer = new AwtTextMeasurer();

  /** A longer text is wider, and the width adds up for a text which repeats. */
  @Test
  void widthGrowsWithText() {
    double one = measurer.width("m", false);
    assertTrue(one > 0);
    assertTrue(measurer.width("mm", false) > one);
    assertEquals(2 * one, measurer.width("mm", false), 0.5);
  }

  /** Bold text is not narrower than the same text in plain. */
  @Test
  void boldNotNarrower() {
    assertTrue(measurer.width("Hilfeeintrag", true) >= measurer.width("Hilfeeintrag", false));
  }

  /** A line is higher than its baseline is far from the top, so a baseline lies inside of the line. */
  @Test
  void ascentInsideLine() {
    assertTrue(measurer.ascent() > 0);
    assertTrue(measurer.lineHeight() > measurer.ascent());
  }

  /** The measure doesn't change from one call to the next, the layout relies on that. */
  @Test
  void measuresAreStable() {
    assertEquals(measurer.width("Versionsnummer", false), measurer.width("Versionsnummer", false));
  }

  /** The shipped font is found on the class path, and the SVG names it by the family which the renderers look up. */
  @Test
  void shippedFontLoaded() {
    assertEquals(FontFamily_V001.ROBOTO.sourceName, DiagramFonts.plain(11).getFamily());
    assertEquals(FontFamily_V001.ROBOTO.sourceName, DiagramFonts.bold(11).getFamily());
  }
}
