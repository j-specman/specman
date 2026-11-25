package classman.editor;

import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.AwtTextMeasurer;
import classman.render.ClassDiagramLayouter;
import classman.render.Scene;
import classman.render.SceneSvgWriter;
import classman.render.SvgFrame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiagramExporterTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String SOURCE = "classdiagram {\nclass Customer (11111111)\n}";

  @TempDir
  File folder;

  /** The picture file has the size of the diagram at the zoom of the editor. */
  @Test
  void pngInCurrentZoom() throws Exception {
    Scene scene = scene();
    File file = new File(folder, "diagram.png");

    new DiagramExporter(scene, 1.5, SOURCE).write(ExportFormat.PNG, file);

    BufferedImage image = ImageIO.read(file);
    assertEquals(Math.round(SvgFrame.of(scene).width * 1.5), image.getWidth());
    assertEquals(Math.round(SvgFrame.of(scene).height * 1.5), image.getHeight());
  }

  /** JPEG has no transparency, and its file can be read back with the size of the diagram. */
  @Test
  void jpegInCurrentZoom() throws Exception {
    Scene scene = scene();
    File file = new File(folder, "diagram.jpg");

    new DiagramExporter(scene, 2, SOURCE).write(ExportFormat.JPEG, file);

    BufferedImage image = ImageIO.read(file);
    assertNotNull(image);
    assertEquals(Math.round(SvgFrame.of(scene).width * 2), image.getWidth());
  }

  /** The SVG is the one which the PDF export places. */
  @Test
  void svgOfTheScene() throws Exception {
    Scene scene = scene();
    File file = new File(folder, "diagram.svg");

    new DiagramExporter(scene, 2, SOURCE).write(ExportFormat.SVG, file);

    assertEquals(new SceneSvgWriter().write(scene), Files.readString(file.toPath(), StandardCharsets.UTF_8));
  }

  /** The Classman file holds the source as it is, with its umlauts. */
  @Test
  void classmanSourceAsItIs() throws Exception {
    File file = new File(folder, "diagram.cld");
    String source = "classdiagram {\nclass Käufer (11111111)\n}";

    new DiagramExporter(scene(), 1, source).write(ExportFormat.CLASSMAN, file);

    assertEquals(source, Files.readString(file.toPath(), StandardCharsets.UTF_8));
  }

  /** The extension of a file name tells the format, whatever its case; jpg and jpeg are the same. */
  @Test
  void formatByExtension() {
    assertEquals(ExportFormat.PNG, ExportFormat.ofFileName("a.PNG"));
    assertEquals(ExportFormat.JPEG, ExportFormat.ofFileName("a.jpeg"));
    assertEquals(ExportFormat.JPEG, ExportFormat.ofFileName("a.b.jpg"));
    assertEquals(ExportFormat.SVG, ExportFormat.ofFileName("a.svg"));
    assertEquals(ExportFormat.CLASSMAN, ExportFormat.ofFileName("a.cld"));
    assertNull(ExportFormat.ofFileName("a.txt"));
    assertNull(ExportFormat.ofFileName("a"));
  }

  private static Scene scene() throws Exception {
    return new ClassDiagramLayouter(new AwtTextMeasurer()).layout(
      new ClassDiagramParser_V001().parse(SOURCE));
  }
}
