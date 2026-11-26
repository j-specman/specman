package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.svg.converter.SvgConverter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static classman.render.SceneTestHelper.all;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The icon of a stereotype in what leaves the editor: the picture, the SVG file, and the PDF which is made of the SVG. */
class StereotypeIconOutputTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final int BLUE = 0xFF0000FF;
  private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\">"
    + "<rect width=\"10\" height=\"10\" fill=\"#0000ff\"/></svg>";
  private static final Map<String, StereotypeLook> LOOKS = Map.of("With_Icon", new StereotypeLook("", VectorImage.of(SVG)));
  private static final ClassDiagramLayouter LAYOUTER =
    new ClassDiagramLayouter(new FixedTextMeasurer(), ChangeColors.NONE, LOOKS::get);

  private static Scene scene() throws ClassDiagramParseException {
    return LAYOUTER.layout(new ClassDiagramParser_V001().parse("classdiagram {\n  class A at(0, 0) <<:With_Icon>>\n}"));
  }

  @Test
  void pictureShowsIcon() throws ClassDiagramParseException {
    Scene scene = scene();
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);
    SvgFrame frame = SvgFrame.of(scene);
    double zoom = 2;

    BufferedImage picture = SceneBitmap.of(scene, zoom);

    int x = (int) ((icon.x + icon.width / 2 - frame.x) * zoom);
    int y = (int) ((icon.y + icon.height / 2 - frame.y) * zoom);
    assertEquals(BLUE, picture.getRGB(x, y));
  }

  @Test
  void svgFileHoldsIconAsPng() throws ClassDiagramParseException {
    Scene scene = scene();
    SceneVectorImage icon = all(scene, SceneVectorImage.class).get(0);

    String svg = new SceneSvgWriter().write(scene);

    assertTrue(svg.contains("<image x=\"" + trimmed(icon.x)), svg);
    assertTrue(svg.contains("href=\"data:image/png;base64,"), svg);
  }

  /** The PDF export converts the SVG of the diagram with iText, which has to find the icon as an image. */
  @Test
  void pdfConversionFindsIconAsImage() throws ClassDiagramParseException, IOException {
    String svg = new SceneSvgWriter().write(scene());

    try (PdfDocument pdf = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()))) {
      PdfFormXObject picture = SvgConverter.convertToXObject(
        new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), pdf);

      var xObjects = picture.getResources().getResource(PdfName.XObject);
      assertFalse(xObjects == null || xObjects.isEmpty(), "No image in the converted picture");
    }
  }

  private static String trimmed(double value) {
    return new java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }
}
