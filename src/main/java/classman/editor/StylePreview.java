package classman.editor;

import classman.model.v001.ClassStyleModel_V001;
import classman.render.AbstractSceneElement;
import classman.render.ClassBoxLayouter;
import classman.render.DiagramRenderer;
import classman.render.DiagramStyle;
import classman.render.Scene;
import classman.render.SceneGraphicsPainter;
import classman.render.SvgFrame;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Shows a class in a style, as the diagram would draw it, or as if a change set had added the class and marked three
 * of its attributes. Both marks at once never occur in a diagram, because the frame of an added class stands for
 * everything in it. Here they are shown together so that the user sees all colors of a change set next to the
 * colors of the style. The picture is painted smaller if it doesn't fit. */
class StylePreview extends JComponent {
  private static final Dimension SIZE = new Dimension(260, 170);

  private final DiagramRenderer renderer;
  private final SceneGraphicsPainter painter = new SceneGraphicsPainter();
  private ClassStyleModel_V001 style = new ClassStyleModel_V001();
  private String changeSet;

  StylePreview(DiagramRenderer renderer) {
    this.renderer = renderer;
    setOpaque(true);
    setBackground(Color.WHITE);
    setPreferredSize(SIZE);
  }

  /** @param changeSet the change set whose marks are shown, null for none */
  void show(ClassStyleModel_V001 newStyle, String newChangeSet) {
    style = newStyle.copy();
    changeSet = newChangeSet;
    repaint();
  }

  /** @return what the preview paints, without the margin */
  Scene scene() {
    Scene laidOut = renderer.layout(StylePreviewModel.of(style, changeSet));
    if (changeSet == null) {
      return laidOut;
    }
    Rectangle2D.Double area = laidOut.elementAreas.get(0).area;
    List<AbstractSceneElement> elements = new ArrayList<>();
    elements.add(ClassBoxLayouter.frameOf(area.width, area.height, renderer.strongColorOf(changeSet))
      .translated(area.x, area.y));
    elements.addAll(laidOut.elements);
    double reach = DiagramStyle.FRAME_STROKE_WIDTH / 2;
    return new Scene(elements, laidOut.x - reach, laidOut.y - reach, laidOut.width + 2 * reach,
      laidOut.height + 2 * reach);
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
