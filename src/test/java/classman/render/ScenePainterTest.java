package classman.render;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParser_V001;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The editor paints the scene itself, so the picture has to be the one the SVG path shows. */
class ScenePainterTest {

  static {
    // The fonts touch AWT, which reads this property once. Without it, headless tests running later in the same JVM fail.
    System.setProperty("java.awt.headless", "true");
  }

  /** A pixel differs if one of its color channels differs by more than this. */
  private static final int CHANNEL_THRESHOLD = 64;
  /** Measured 0.3 to 0.6 percent: the SVG renderer places centered and right-aligned texts about half a pixel differently. */
  private static final double MAX_DIFFERING_SHARE = 0.01;
  /** How far the bounding boxes of the drawn parts may be apart, in pixels of the picture. */
  private static final int MAX_BOUNDS_DEVIATION = 2;
  private static final int NOT_WHITE_BELOW = 250;

  private static final Map<String, String> CHANGE_COLORS = Map.of(
      "yellow", "#ffff00", "lilac", "#c8aaf0", "blue", "#82d2ff", "orange", "#ffdc96", "pink", "#ffaaaa");

  /** The painter shows the same picture as the SVG at 100% and 200%, as far as anti-aliasing allows. */
  @ParameterizedTest
  @CsvSource({"benchmark,1", "benchmark,2", "change-colors,1", "change-colors,2"})
  void samePictureAsSvg(String example, double zoom) throws Exception {
    Scene scene = sceneOf(example);
    BufferedImage viaSvg = viaSvg(scene, zoom);
    BufferedImage painted = painted(scene, viaSvg.getWidth(), viaSvg.getHeight());

    Rectangle svgBounds = drawnBounds(viaSvg);
    Rectangle paintedBounds = drawnBounds(painted);
    double share = differingShare(viaSvg, painted);
    if (!sameWithin(svgBounds, paintedBounds) || share > MAX_DIFFERING_SHARE) {
      dump(example + "-" + zoom, viaSvg, painted);
    }
    assertTrue(sameWithin(svgBounds, paintedBounds), "bounds " + svgBounds + " vs " + paintedBounds);
    assertTrue(share <= MAX_DIFFERING_SHARE, "differing share " + share);
  }

  private static Scene sceneOf(String example) throws Exception {
    ClassDiagramModel_V001 model = new ClassDiagramParser_V001().parse(exampleSource(example));
    return new ClassDiagramLayouter(new AwtTextMeasurer(), CHANGE_COLORS::get).layout(model);
  }

  private static String exampleSource(String example) throws IOException {
    try (InputStream in = ScenePainterTest.class.getResourceAsStream("/classman/model/v001/" + example + ".cld")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** The picture as the Specman display path makes it: the SVG shown by a FlatSVGIcon, scaled to the zoom. */
  private static BufferedImage viaSvg(Scene scene, double zoom) throws IOException {
    byte[] svg = new SceneSvgWriter().write(scene).getBytes(StandardCharsets.UTF_8);
    FlatSVGIcon icon = new FlatSVGIcon(new ByteArrayInputStream(svg));
    FlatSVGIcon scaled = icon.derive((int) Math.round(icon.getIconWidth() * zoom),
        (int) Math.round(icon.getIconHeight() * zoom));
    BufferedImage image = whiteImage(scaled.getIconWidth(), scaled.getIconHeight());
    Graphics2D g = image.createGraphics();
    scaled.paintIcon(null, g, 0, 0);
    g.dispose();
    return image;
  }

  /** The picture of the painter in an image of the given size: the frame of the scene scaled to fill it. */
  private static BufferedImage painted(Scene scene, int width, int height) {
    BufferedImage image = whiteImage(width, height);
    Graphics2D g = image.createGraphics();
    SvgFrame frame = SvgFrame.of(scene);
    if (frame.width > 0 && frame.height > 0) {
      g.scale(width / frame.width, height / frame.height);
    }
    g.translate(-frame.x, -frame.y);
    new SceneGraphicsPainter().paint(scene, g);
    g.dispose();
    return image;
  }

  private static BufferedImage whiteImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, width, height);
    g.dispose();
    return image;
  }

  private static boolean sameWithin(Rectangle a, Rectangle b) {
    return Math.abs(a.x - b.x) <= MAX_BOUNDS_DEVIATION && Math.abs(a.y - b.y) <= MAX_BOUNDS_DEVIATION
        && Math.abs(a.width - b.width) <= MAX_BOUNDS_DEVIATION && Math.abs(a.height - b.height) <= MAX_BOUNDS_DEVIATION;
  }

  /** The smallest rectangle around all pixels which are not white. */
  private static Rectangle drawnBounds(BufferedImage image) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (isDrawn(image.getRGB(x, y))) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }
    return maxX < 0 ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
  }

  private static boolean isDrawn(int rgb) {
    return channel(rgb, 16) < NOT_WHITE_BELOW || channel(rgb, 8) < NOT_WHITE_BELOW || channel(rgb, 0) < NOT_WHITE_BELOW;
  }

  private static int channel(int rgb, int shift) {
    return (rgb >> shift) & 0xff;
  }

  /**
   * The share of drawn pixels which have no match in the other picture. A pixel matches if the other picture has a
   * similar pixel at most one pixel away, because the rasterizers place anti-aliased edges and glyphs differently
   * by a fraction of a pixel.
   */
  private static double differingShare(BufferedImage a, BufferedImage b) {
    int drawn = 0;
    int unmatched = 0;
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (isDrawn(a.getRGB(x, y)) || isDrawn(b.getRGB(x, y))) {
          drawn++;
          if (!matchesNearby(a, b, x, y) || !matchesNearby(b, a, x, y)) {
            unmatched++;
          }
        }
      }
    }
    return drawn == 0 ? 0 : (double) unmatched / drawn;
  }

  /** True if the pixel of one picture has a similar pixel within one pixel distance in the other. */
  private static boolean matchesNearby(BufferedImage from, BufferedImage in, int x, int y) {
    int pixel = from.getRGB(x, y);
    for (int dy = -1; dy <= 1; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        int nx = x + dx;
        int ny = y + dy;
        if (nx >= 0 && ny >= 0 && nx < in.getWidth() && ny < in.getHeight() && !differs(pixel, in.getRGB(nx, ny))) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean differs(int pixelA, int pixelB) {
    return Math.abs(channel(pixelA, 16) - channel(pixelB, 16)) > CHANNEL_THRESHOLD
        || Math.abs(channel(pixelA, 8) - channel(pixelB, 8)) > CHANNEL_THRESHOLD
        || Math.abs(channel(pixelA, 0) - channel(pixelB, 0)) > CHANNEL_THRESHOLD;
  }
  /** Writes both pictures and a difference picture (red where they differ) to the temp folder to have a look at. */
  private static void dump(String name, BufferedImage viaSvg, BufferedImage painted) throws IOException {
    File folder = new File(System.getProperty("java.io.tmpdir"), "classman-painter");
    folder.mkdirs();
    BufferedImage diff = whiteImage(viaSvg.getWidth(), viaSvg.getHeight());
    for (int y = 0; y < diff.getHeight(); y++) {
      for (int x = 0; x < diff.getWidth(); x++) {
        diff.setRGB(x, y, !matchesNearby(viaSvg, painted, x, y) || !matchesNearby(painted, viaSvg, x, y) ? Color.RED.getRGB()
            : isDrawn(viaSvg.getRGB(x, y)) || isDrawn(painted.getRGB(x, y)) ? Color.LIGHT_GRAY.getRGB() : Color.WHITE.getRGB());
      }
    }
    ImageIO.write(viaSvg, "png", new File(folder, name + "-svg.png"));
    ImageIO.write(painted, "png", new File(folder, name + "-painted.png"));
    ImageIO.write(diff, "png", new File(folder, name + "-diff.png"));
  }
}
