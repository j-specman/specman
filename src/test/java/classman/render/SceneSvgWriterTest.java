package classman.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The scenes in these tests are built by hand, so only the writing of the SVG is tested. */
class SceneSvgWriterTest {

  private final SceneSvgWriter writer = new SceneSvgWriter();

  private static Scene sceneOf(AbstractSceneElement... elements) {
    return new Scene(List.of(elements), 10, 20, 100, 50);
  }

  /** The SVG is as large as the scene plus a margin on each side, and its view box starts at the scene minus the margin. */
  @Test
  void sizeAndViewBoxFromSceneBounds() {
    String svg = writer.write(sceneOf());
    assertTrue(svg.contains("width=\"120\""), svg);
    assertTrue(svg.contains("height=\"70\""), svg);
    assertTrue(svg.contains("viewBox=\"0 10 120 70\""), svg);
  }

  /** A rectangle with fill and outline. */
  @Test
  void rectangleWithFillAndStroke() {
    String svg = writer.write(sceneOf(new SceneRectangle(1, 2, 30, 40, "#fdd34d", "#000000")));
    assertTrue(svg.contains("<rect x=\"1\" y=\"2\" width=\"30\" height=\"40\" fill=\"#fdd34d\" stroke=\"#000000\"/>"), svg);
  }

  /** A shape without fill says fill none, else the browser would fill it black. */
  @Test
  void missingFillIsNone() {
    String svg = writer.write(sceneOf(new SceneRectangle(0, 0, 1, 1, null, "#000000")));
    assertTrue(svg.contains("fill=\"none\""), svg);
  }

  /** Numbers are written without trailing zeros, and always with a point, whatever the locale. */
  @Test
  void numberFormat() {
    String svg = writer.write(sceneOf(new SceneRectangle(1.5, 2.25, 3.0, 4.333333, "#fff", null)));
    assertTrue(svg.contains("x=\"1.5\" y=\"2.25\" width=\"3\" height=\"4.33\""), svg);
  }

  /** A text has its anchor, its weight and the font of the diagram, and its characters are escaped. */
  @Test
  void textWithAnchorAndEscaping() {
    String svg = writer.write(sceneOf(new SceneText(5, 6, "a<b & \"c\"", true, TextAnchor.MIDDLE, 50)));
    assertTrue(svg.contains("text-anchor=\"middle\""), svg);
    assertTrue(svg.contains("font-weight=\"bold\""), svg);
    assertTrue(svg.contains("font-family=\"" + DiagramStyle.SVG_FONT_FAMILIES + "\""), svg);
    assertTrue(svg.contains(">a&lt;b &amp; &quot;c&quot;</text>"), svg);
  }

  /** A text which isn't bold doesn't say so, and a text at its start is anchored at the start. */
  @Test
  void plainTextAtStart() {
    String svg = writer.write(sceneOf(new SceneText(5, 6, "x", false, TextAnchor.START, 10)));
    assertTrue(svg.contains("text-anchor=\"start\""), svg);
    assertFalse(svg.contains("font-weight"), svg);
  }

  /** A polyline has its points, no fill, and a dash pattern if it is dashed. */
  @Test
  void dashedPolyline() {
    String svg = writer.write(sceneOf(new ScenePolyline(new double[]{0, 10, 10}, new double[]{0, 0, 5}, true)));
    assertTrue(svg.contains("<polyline points=\"0,0 10,0 10,5\" fill=\"none\" stroke=\"#000000\""), svg);
    assertTrue(svg.contains("stroke-dasharray"), svg);
  }

  /** A solid polyline has no dash pattern. */
  @Test
  void solidPolyline() {
    String svg = writer.write(sceneOf(new ScenePolyline(new double[]{0, 10}, new double[]{0, 0}, false)));
    assertFalse(svg.contains("stroke-dasharray"), svg);
  }

  /** A polygon has its points, its fill and its outline. */
  @Test
  void polygonPoints() {
    String svg = writer.write(sceneOf(new ScenePolygon(new double[]{0, 4, 4}, new double[]{0, 0, 3}, "#ffffff", "#000000")));
    assertTrue(svg.contains("<polygon points=\"0,0 4,0 4,3\" fill=\"#ffffff\" stroke=\"#000000\"/>"), svg);
  }

  /** The parts come in the order of the scene, so later ones are drawn above earlier ones. */
  @Test
  void drawingOrder() {
    String svg = writer.write(sceneOf(new SceneRectangle(0, 0, 1, 1, "#111111", null),
        new SceneRectangle(0, 0, 1, 1, "#222222", null)));
    assertTrue(svg.indexOf("#111111") < svg.indexOf("#222222"), svg);
  }
}
