package classman.editor;

import classman.render.Scene;
import classman.render.SceneBitmap;
import classman.render.SceneSvgWriter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Writes a diagram to a file. Pictures of pixels have the zoom which the editor shows the diagram in, the SVG is the
 * one which the PDF export places, and the Classman format is the source of the diagram as it is. */
class DiagramExporter {
  private static final float JPEG_QUALITY = 0.92f;

  private final Scene scene;
  private final double zoom;
  private final String source;

  DiagramExporter(Scene scene, double zoom, String source) {
    this.scene = scene;
    this.zoom = zoom;
    this.source = source;
  }

  void write(ExportFormat format, File file) throws IOException {
    switch (format) {
      case PNG -> writePng(SceneBitmap.of(scene, zoom), file);
      case JPEG -> writeJpeg(SceneBitmap.of(scene, zoom), file);
      case SVG -> writeText(new SceneSvgWriter().write(scene), file);
      case CLASSMAN -> writeText(source, file);
    }
  }

  private static void writeText(String text, File file) throws IOException {
    Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
  }

  private static void writePng(BufferedImage image, File file) throws IOException {
    if (!ImageIO.write(image, "png", file)) {
      throw new IOException("No PNG writer available");
    }
  }

  private static void writeJpeg(BufferedImage image, File file) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
      ImageWriteParam parameters = writer.getDefaultWriteParam();
      parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      parameters.setCompressionQuality(JPEG_QUALITY);
      writer.setOutput(out);
      writer.write(null, new IIOImage(image, null, null), parameters);
    }
    finally {
      writer.dispose();
    }
  }
}
