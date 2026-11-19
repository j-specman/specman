package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The texts at relations. A and B stacked: 40 wide and 56 high, A at (0, 0), B at (0, 96), so the line
 * runs from (20, 56) down to (20, 96). Placed at (0, 0) and (300, 0), the line runs from (40, 28) to (300, 28).
 */
class RelationTextTest {

  private static final String STACKED = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n";
  private static final String SIDE_BY_SIDE = "class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 0)\n";

  private static void assertText(SceneText text, double x, double y, TextAnchor anchor) {
    assertEquals(x, text.x, 1e-9, "x of " + text.text);
    assertEquals(y, text.y, 1e-9, "baseline of " + text.text);
    assertEquals(anchor, text.anchor);
  }

  // ---- multiplicities ----

  /** At a line running down from a box, the multiplicity stands right of the line, just below the border. */
  @Test
  void multiplicityBelowBorderRightOfLine() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association aaaaaaaa 1 -> * bbbbbbbb");
    assertText(textNamed(scene, "1"), 28, 71, TextAnchor.START);
  }

  /** At a line coming up to a box, the multiplicity stands right of the line, just above the border. */
  @Test
  void multiplicityAboveBorderRightOfLine() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association aaaaaaaa 1 -> * bbbbbbbb");
    assertText(textNamed(scene, "*"), 28, 94, TextAnchor.START);
  }

  /** At a horizontal line the multiplicities stand above it, next to the borders. */
  @Test
  void multiplicitiesAboveHorizontalLine() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa 0..1 -> 1..* bbbbbbbb");
    assertText(textNamed(scene, "0..1"), 44, 20, TextAnchor.START);
    assertText(textNamed(scene, "1..*"), 296, 20, TextAnchor.END);
  }

  /** Beside a line which runs down to the right, the text stands right of the line's lowest point under it. */
  @Test
  void multiplicityClearsSlantedDownLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(100, 200)\nassociation aaaaaaaa 1 -> * bbbbbbbb");
    assertText(textNamed(scene, "1"), 52, 71, TextAnchor.START);
  }

  /** Above a line which rises to the right, the text stands above the line's highest point under it. */
  @Test
  void multiplicityClearsRisingLine() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, -100)\nassociation aaaaaaaa 0..1 -> * bbbbbbbb");
    SceneText text = textNamed(scene, "0..1");
    assertEquals(44, text.x, 1e-9);
    assertEquals(-1.33, text.y, 0.01);
  }

  /** A multiplicity which the source leaves out isn't drawn. */
  @Test
  void noMultiplicityNoText() throws ClassDiagramParseException {
    assertTrue(all(layout(STACKED + "association aaaaaaaa -> bbbbbbbb"), SceneText.class).stream()
        .noneMatch(text -> !text.text.equals("A") && !text.text.equals("B")));
  }

  // ---- labels ----

  /** A label on a horizontal line is centered above the middle of the line. */
  @Test
  void labelAboveHorizontalLine() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\"");
    assertText(textNamed(scene, "owns"), 170, 22, TextAnchor.MIDDLE);
  }

  /** A label with several lines grows upwards from the line, the last line closest to it. */
  @Test
  void multiLineLabelAboveLine() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"first\\nsecond\"");
    assertText(textNamed(scene, "first"), 170, 2, TextAnchor.MIDDLE);
    assertText(textNamed(scene, "second"), 170, 22, TextAnchor.MIDDLE);
  }

  /** A label on a vertical line stands right of the line, centered on its middle. */
  @Test
  void labelRightOfVerticalLine() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association aaaaaaaa -- bbbbbbbb \"two\\nlines\"");
    assertText(textNamed(scene, "two"), 26, 71, TextAnchor.START);
    assertText(textNamed(scene, "lines"), 26, 91, TextAnchor.START);
  }

  /** On a route the label goes to the middle of its longest part. */
  @Test
  void labelOnLongestSegment() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(200, 100)\n"
        + "association aaaaaaaa -- bbbbbbbb \"on route\"\n  route aaaaaaaa.bottom@10 (10, 150) bbbbbbbb.left@20");
    assertText(textNamed(scene, "on route"), 105, 129, TextAnchor.MIDDLE);
  }

  /** The scene is as large as it takes to show a label which reaches beyond the boxes. */
  @Test
  void sceneCoversLongLabel() throws ClassDiagramParseException {
    String label = "x".repeat(50);
    Scene scene = layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"" + label + "\"");
    assertEquals(170 - 250, scene.x, 1e-9);
    assertTrue(scene.x + scene.width >= 170 + 250);
  }

  /** The text knows its width, the layout needs it to place and measure. */
  @Test
  void textKnowsItsWidth() throws ClassDiagramParseException {
    List<SceneText> texts = all(layout(SIDE_BY_SIDE + "association aaaaaaaa -- bbbbbbbb \"owns\""), SceneText.class);
    assertEquals(40, texts.stream().filter(text -> text.text.equals("owns")).findFirst().get().width, 1e-9);
  }
}
