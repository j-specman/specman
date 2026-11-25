package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classes are 40 wide and 56 high. A and B stand left and right at y = 100, so the first relation runs along y = 128
 * from x = 40 to x = 300. C and D stand above and below at x = 130, so the second relation runs along x = 150.
 */
class CrossingGapsTest {

  private static final String HORIZONTAL = "class A (aaaaaaaa) at(0, 100)\nclass B (bbbbbbbb) at(300, 100)\n"
    + "association aaaaaaaa -- bbbbbbbb\n";
  private static final String VERTICAL = "class C (cccccccc) at(130, 0)\nclass D (dddddddd) at(130, 260)\n"
    + "association cccccccc -- dddddddd\n";

  private static List<ScenePolyline> lines(Scene scene) {
    return all(scene, ScenePolyline.class);
  }

  /** Where a later line crosses, the line gets a gap, the same on both sides. */
  @Test
  void earlierLineHasGapWhereLaterOneCrosses() throws ClassDiagramParseException {
    List<ScenePolyline> lines = lines(layout(HORIZONTAL + VERTICAL));
    assertEquals(3, lines.size());
    assertEquals(40, lines.get(0).xs[0], 1e-9);
    assertEquals(300, lines.get(1).xs[1], 1e-9);
    double leftEnd = lines.get(0).xs[1];
    double rightStart = lines.get(1).xs[0];
    assertTrue(leftEnd < 150 && rightStart > 150);
    assertEquals(150 - leftEnd, rightStart - 150, 1e-9);
  }

  /** The line which is drawn later runs over the other one without a gap. */
  @Test
  void laterLineStaysWhole() throws ClassDiagramParseException {
    List<ScenePolyline> lines = lines(layout(HORIZONTAL + VERTICAL));
    assertEquals(56, lines.get(2).ys[0], 1e-9);
    assertEquals(260, lines.get(2).ys[1], 1e-9);
  }

  /** What the user selects and drags by is the whole line, the gap is only for the picture. */
  @Test
  void selectableLineStaysWhole() throws ClassDiagramParseException {
    Scene scene = layout(HORIZONTAL + VERTICAL);
    ScenePolyline whole = scene.relationLines.get(0).line;
    assertEquals(40, whole.xs[0], 1e-9);
    assertEquals(300, whole.xs[1], 1e-9);
  }

  /** A crossing so close to the end of a line that its mark is there gets no gap. */
  @Test
  void noGapNearEndOfLine() throws ClassDiagramParseException {
    String shortVertical = "class C (cccccccc) at(130, 0)\nclass D (dddddddd) at(130, 132)\n"
      + "association cccccccc -- dddddddd\n";
    List<ScenePolyline> lines = lines(layout(HORIZONTAL + shortVertical));
    assertEquals(2, lines.size());
  }

  /** Lines which don't cross stay whole. */
  @Test
  void noGapWithoutCrossing() throws ClassDiagramParseException {
    String besideVertical = "class C (cccccccc) at(330, 0)\nclass D (dddddddd) at(330, 260)\n"
      + "association cccccccc -- dddddddd\n";
    assertEquals(2, lines(layout(HORIZONTAL + besideVertical)).size());
  }
}
