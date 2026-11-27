package specman.editarea.image.classman;

import classman.render.DiagramRenderer;
import classman.render.Scene;

import static specman.i18n.Messages.i18n;

/** Gives Classman what only Specman knows for rendering a diagram: the colors of the change sets and the language. */
public final class ClassmanRenderer {
  private ClassmanRenderer() {}

  /** @return the scene of the diagram, or of a picture of the problem if the source can't be drawn */
  public static Scene renderScene(String source) {
    return renderer().render(source);
  }

  /** @return the scene which stands for a linked diagram that can't be shown */
  static Scene renderLinkPlaceholder(String link, String problem) {
    return renderer().renderLinkPlaceholder(link, problem);
  }

  static DiagramRenderer renderer() {
    return new DiagramRenderer(new ClassmanChangeColors(), new ClassmanStereotypes(), i18n("msg.classman.render.error"));
  }
}
