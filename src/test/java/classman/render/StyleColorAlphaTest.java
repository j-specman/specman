package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static classman.render.SceneTestHelper.all;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The colors of a style can be transparent: #rrggbbaa. The painter blends them with what is behind, the SVG says the
 * opacity in an attribute of its own. */
class StyleColorAlphaTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String HALF_RED_CLASS = "class A at(0, 0) width=100 style(fill=\"#ff000080\")";

  @Test
  void colorWithAlphaIsReadAndWrittenAsItIs() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A at(0, 0) style(fill=\"#ff000080\", textcolor=\"#00ff0040\", linecolor=\"#0000ff10\")\n}";

    String written = new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));

    assertTrue(written.contains("\"#ff000080\""), written);
    assertTrue(written.contains("\"#00ff0040\""), written);
    assertTrue(written.contains("\"#0000ff10\""), written);
  }

  @Test
  void colorWithOtherLengthIsRejected() {
    assertThrows(ClassDiagramParseException.class, () -> new ClassDiagramParser_V001()
      .parse("classdiagram {\n  class A style(fill=\"#ff0000f\")\n}"));
    assertThrows(ClassDiagramParseException.class, () -> new ClassDiagramParser_V001()
      .parse("classdiagram {\n  class A style(fill=\"#ff00\")\n}"));
  }

  @Test
  void painterBlendsTransparentFillWithWhite() throws ClassDiagramParseException {
    Scene scene = SceneTestHelper.layout(HALF_RED_CLASS);
    SceneRectangle box = all(scene, SceneRectangle.class).get(0);
    SvgFrame frame = SvgFrame.of(scene);

    BufferedImage picture = SceneBitmap.of(scene, 1);

    int pixel = picture.getRGB((int) (box.x + box.width / 2 - frame.x), (int) (box.y + 3 - frame.y));
    int green = (pixel >> 8) & 0xff;
    assertTrue(green > 100 && green < 160, "half red on white is pink, green was " + green);
    assertEquals(0xff, (pixel >> 16) & 0xff);
  }

  @Test
  void svgSaysOpacityInAttributeOfItsOwn() throws ClassDiagramParseException {
    String svg = new SceneSvgWriter().write(SceneTestHelper.layout(HALF_RED_CLASS));

    assertTrue(svg.contains("fill=\"#ff0000\" fill-opacity=\"0.5\""), svg);
  }

  @Test
  void svgOfTransparentLineAndTextSaysOpacityToo() throws ClassDiagramParseException {
    String svg = new SceneSvgWriter().write(SceneTestHelper.layout(
      "class A at(0, 0) style(linecolor=\"#0000ff80\", textcolor=\"#00ff0080\") {\n  -x\n}"));

    assertTrue(svg.contains("stroke=\"#0000ff\" stroke-opacity=\"0.5\""), svg);
    assertTrue(svg.contains("fill=\"#00ff00\" fill-opacity=\"0.5\""), svg);
  }

  @Test
  void opaqueColorsWriteNoOpacity() throws ClassDiagramParseException {
    String svg = new SceneSvgWriter().write(SceneTestHelper.layout("class A at(0, 0) style(fill=\"#ff0000\")"));

    assertTrue(!svg.contains("opacity"), svg);
  }
}
