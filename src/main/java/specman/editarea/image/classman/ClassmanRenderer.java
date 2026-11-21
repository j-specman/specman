package specman.editarea.image.classman;

import classman.render.DiagramRenderer;
import classman.render.Scene;
import specman.ChangeSet;

import java.awt.Color;

import static specman.i18n.Messages.i18n;

/** Gives Classman what only Specman knows for rendering a diagram: the colors of the change sets and the language. */
public final class ClassmanRenderer {
  private ClassmanRenderer() {}

  /** @return the scene of the diagram, or of a picture of the problem if the source can't be drawn */
  public static Scene renderScene(String source) {
    return renderer().render(source);
  }

  static DiagramRenderer renderer() {
    return new DiagramRenderer(ClassmanRenderer::strongColorOf, i18n("msg.classman.render.error"));
  }

  /** @return the color of the change set as #rrggbb, or null if Specman has no change set of that name */
  private static String strongColorOf(String changeSetName) {
    ChangeSet changeSet = ChangeSet.fromName(changeSetName);
    if (changeSet == null) {
      return null;
    }
    Color color = changeSet.textColor();
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }
}
