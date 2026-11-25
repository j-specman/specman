package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.render.Scene;
import classman.render.SceneGraphicsPainter;
import classman.render.SvgFrame;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.BiFunction;

/** Shows an element in a style, as the diagram would draw it, or as if a change set had marked it. The picture is
 * painted smaller if it doesn't fit. What the picture is, is the business of the dialog which owns the preview. */
class StylePreview extends JComponent {
  private static final Dimension SIZE = new Dimension(260, 170);

  private final BiFunction<ElementStyleModel_V001, String, Scene> scenes;
  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private ElementStyleModel_V001 style = new ElementStyleModel_V001();
  private String changeSet;

  /** @param scenes gives the picture for a style and a change set, null for the unmarked element */
  StylePreview(BiFunction<ElementStyleModel_V001, String, Scene> scenes) {
    this.scenes = scenes;
    setOpaque(true);
    setBackground(Color.WHITE);
    setPreferredSize(SIZE);
  }

  /** @param newChangeSet the change set whose marks are shown, null for none */
  void show(ElementStyleModel_V001 newStyle, String newChangeSet) {
    style = newStyle.copy();
    changeSet = newChangeSet;
    repaint();
  }

  /** @return what the preview paints, without the margin */
  Scene scene() {
    return scenes.apply(style, changeSet);
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    Scene scene = scene();
    SvgFrame frame = SvgFrame.of(scene);
    double scale = Math.min(1, Math.min(getWidth() / frame.width, getHeight() / frame.height));
    g.translate((getWidth() - frame.width * scale) / 2, (getHeight() - frame.height * scale) / 2);
    g.scale(scale, scale);
    g.translate(-frame.x, -frame.y);
    painter.paint(scene, g);
    g.dispose();
  }
}
