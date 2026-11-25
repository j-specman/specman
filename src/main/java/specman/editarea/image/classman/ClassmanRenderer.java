package specman.editarea.image.classman;

import classman.render.DiagramRenderer;
import classman.render.Scene;
import classman.render.StereotypeLook;
import specman.metatag.generic.config.MetaTagConfig;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** Gives Classman what only Specman knows for rendering a diagram: the colors of the change sets and the language. */
public final class ClassmanRenderer {
  private ClassmanRenderer() {}

  /** @return the scene of the diagram, or of a picture of the problem if the source can't be drawn */
  public static Scene renderScene(String source) {
    return renderer().render(source);
  }

  static DiagramRenderer renderer() {
    return new DiagramRenderer(new ClassmanChangeColors(), ClassmanRenderer::stereotypeLook, i18n("msg.classman.render.error"));
  }

  /** A stereotype refers to the meta tag configuration of that name, which gives the label. */
  private static StereotypeLook stereotypeLook(String name) {
    MetaTagConfig config = editor().findMetaTagConfig(name);
    return config != null ? new StereotypeLook(config.getLabel()) : null;
  }
}
