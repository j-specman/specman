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

  /** A text is written at the left end the layout gives it, with its weight and the font of the diagram, escaped. */
  @Test
  void textAtLeftEndWithEscaping() {
    String svg = writer.write(sceneOf(new SceneText(50, 6, "a<b & \"c\"", true, TextAnchor.MIDDLE, 40)));
    assertTrue(svg.contains("<text x=\"30\" y=\"6\""), svg);
    assertFalse(svg.contains("text-anchor"), svg);
    assertTrue(svg.contains("font-weight=\"bold\""), svg);
    assertTrue(svg.contains("font-family=\"Roboto, Arial, sans-serif\" font-size=\"12\""), svg);
    assertTrue(svg.contains(">a&lt;b &amp; &quot;c&quot;</text>"), svg);
  }

  /** A text which isn't bold doesn't say so, and a text ending at x starts at x minus its width. */
  @Test
  void plainTextAtEnd() {
    String svg = writer.write(sceneOf(new SceneText(25, 6, "x", false, TextAnchor.END, 10)));
    assertTrue(svg.contains("<text x=\"15\" y=\"6\""), svg);
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

  /** A stroke which isn't the usual one says its width, and a veiled text its color. */
  @Test
  void strokeWidthAndTextFill() {
    String svg = writer.write(sceneOf(new SceneRectangle(0, 0, 10, 10, null, "#ffff00", 12),
        new ScenePolyline(new double[]{0, 10}, new double[]{0, 0}, false, "#ffff00", 9),
        new SceneText(0, 0, "x", false, TextAnchor.START, 10, "#9a9a9a")));
    assertTrue(svg.contains("stroke-width=\"12\""), svg);
    assertTrue(svg.contains("stroke=\"#ffff00\" stroke-width=\"9\""), svg);
    assertFalse(svg.contains("round"), svg);
    assertFalse(svg.contains("rx="), svg);
    assertTrue(svg.contains("fill=\"#9a9a9a\">x</text>"), svg);
  }

  /** Usual strokes and texts say nothing about width, corners or fill, so the SVG stays as it was. */
  @Test
  void usualPartsStaySlim() {
    String svg = writer.write(sceneOf(new SceneRectangle(0, 0, 10, 10, "#ffffff", "#000000"),
        new SceneText(0, 0, "x", false, TextAnchor.START, 10)));
    assertFalse(svg.contains("stroke-width"), svg);
    assertFalse(svg.contains("rx="), svg);
    assertFalse(svg.contains("fill=\"#") && svg.indexOf("fill=\"#", svg.indexOf("<text")) > 0, svg);
  }

  /** The parts come in the order of the scene, so later ones are drawn above earlier ones. */
  @Test
  void drawingOrder() {
    String svg = writer.write(sceneOf(new SceneRectangle(0, 0, 1, 1, "#111111", null),
        new SceneRectangle(0, 0, 1, 1, "#222222", null)));
    assertTrue(svg.indexOf("#111111") < svg.indexOf("#222222"), svg);
  }
}
