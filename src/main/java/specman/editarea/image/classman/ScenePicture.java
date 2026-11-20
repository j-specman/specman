package specman.editarea.image.classman;

import classman.render.Scene;
import classman.render.SceneIcon;
import classman.render.SceneSvgWriter;
import classman.render.SvgFrame;
import specman.editarea.image.Picture;

import javax.swing.Icon;
import java.nio.charset.StandardCharsets;

/** A Classman diagram painted with Graphics2D. Its natural size is the one of the SVG of the scene, which the PDF
 * export places. */
class ScenePicture implements Picture {
  private final Scene scene;
  private final int width;
  private final int height;

  ScenePicture(Scene scene) {
    this.scene = scene;
    SvgFrame frame = SvgFrame.of(scene);
    this.width = (int) Math.round(frame.width);
    this.height = (int) Math.round(frame.height);
  }

  @Override
  public int width() { return width; }

  @Override
  public int height() { return height; }

  @Override
  public Icon scaledIcon(float scale) {
    return new SceneIcon(scene, Math.round(width * scale), Math.round(height * scale));
  }

  /** Written when asked for, as PDF export is rare. */
  @Override
  public byte[] svg() {
    return new SceneSvgWriter().write(scene).getBytes(StandardCharsets.UTF_8);
  }
}
