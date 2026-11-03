package specman.graphics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manual tool, not a test: renders the step icons next to some Material icons at several scale factors
 * (like Windows display scaling) on a light and a dark background into a PNG, to judge their sharpness.
 * Run with {@code mvn -Dtest=IconRenderSheet -Diconsheet.out=<file.png> test}.
 */
@EnabledIfSystemProperty(named = "iconsheet.out", matches = ".+")
class IconRenderSheet {

  private static final double[] SCALES = {1.0, 1.25, 1.5, 2.0};
  private static final int CELL = 64;
  private static final int PREVIEW_ZOOM = 1;

  @Test
  void render() throws IOException {
    List<String> paths = List.of(
        SpecmanIcons.STEP_SIMPLE, SpecmanIcons.STEP_WHILE, SpecmanIcons.STEP_WHILEWHILE, SpecmanIcons.STEP_IFELSE,
        SpecmanIcons.STEP_IF, SpecmanIcons.STEP_CASE, SpecmanIcons.STEP_SUBSEQUENCE, SpecmanIcons.STEP_BREAK,
        SpecmanIcons.STEP_CATCH, SpecmanIcons.STEP_CASE_BRANCH,
        SpecmanIcons.DELETE, SpecmanIcons.TRACK_CHANGES, SpecmanIcons.ACCEPT_CHANGES, SpecmanIcons.TABLE);
    int size = Integer.getInteger("iconsheet.size", SpecmanIcons.SIZE_TOOLBAR);
    int width = paths.size() * CELL;
    int rowHeight = CELL;
    BufferedImage sheet = new BufferedImage(width, rowHeight * SCALES.length * 2, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = sheet.createGraphics();
    int y = 0;
    for (boolean dark : new boolean[] {false, true}) {
      UIManager.put("Label.foreground", dark ? new Color(0xdddddd) : new Color(0x222222));
      for (double scale : SCALES) {
        g.setColor(dark ? new Color(0x3c3f41) : Color.WHITE);
        g.fillRect(0, y, width, rowHeight);
        int x = 0;
        for (String path : paths) {
          int iconSize = path.contains("/specman/") ? Integer.getInteger("iconsheet.stepsize", size) : SpecmanIcons.SIZE_TOOLBAR;
          Icon icon = IconReader.readSvgIcon(path, iconSize);
          Graphics2D cell = (Graphics2D) g.create(x, y, CELL, rowHeight);
          cell.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          cell.scale(scale, scale);
          icon.paintIcon(null, cell, (int) ((CELL - iconSize * scale) / 2 / scale), (int) ((rowHeight - iconSize * scale) / 2 / scale));
          cell.dispose();
          x += CELL;
        }
        y += rowHeight;
      }
    }
    g.dispose();
    ImageIO.write(sheet, "png", new File(System.getProperty("iconsheet.out")));
  }
}
