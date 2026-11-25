package classman.render;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Colors of a diagram are written #rrggbb, or #rrggbbaa if they are not opaque. */
class SceneColorTest {

  @Test
  void opaqueColorHasNoOpacity() {
    assertEquals(new Color(0xf2, 0xe9, 0xc4), SceneColor.parse("#f2e9c4"));
    assertEquals("#f2e9c4", SceneColor.rgb("#f2e9c4"));
    assertNull(SceneColor.opacity("#f2e9c4"));
  }

  @Test
  void fullAlphaIsOpaque() {
    assertEquals(new Color(0xf2, 0xe9, 0xc4), SceneColor.parse("#f2e9c4ff"));
    assertNull(SceneColor.opacity("#f2e9c4ff"));
  }

  @Test
  void colorWithAlpha() {
    assertEquals(new Color(0xff, 0, 0, 0x80), SceneColor.parse("#ff000080"));
    assertEquals("#ff0000", SceneColor.rgb("#ff000080"));
    assertEquals("0.5", SceneColor.opacity("#ff000080"));
  }

  @Test
  void fullyTransparentColor() {
    assertEquals("0", SceneColor.opacity("#ff000000"));
  }

  @Test
  void colorIsWrittenWithAlphaOnlyIfNotOpaque() {
    assertEquals("#ff0000", SceneColor.of(new Color(255, 0, 0)));
    assertEquals("#ff000080", SceneColor.of(new Color(255, 0, 0, 128)));
  }

  @Test
  void validColors() {
    assertTrue(SceneColor.isValid("#aabbcc"));
    assertTrue(SceneColor.isValid("#AABBCC80"));
    assertFalse(SceneColor.isValid("#aabbc"));
    assertFalse(SceneColor.isValid("#aabbccd"));
    assertFalse(SceneColor.isValid("aabbcc"));
    assertFalse(SceneColor.isValid("#gggggg"));
  }

  @Test
  void blendKeepsAlphaOfTheFirstColor() {
    assertEquals("#ff8080" + "80", ColorMix.blend("#ff000080", "#ffffff", 0.5));
    assertEquals("#ff8080", ColorMix.blend("#ff0000", "#ffffff", 0.5));
  }
}
