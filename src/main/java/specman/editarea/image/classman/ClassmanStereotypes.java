package specman.editarea.image.classman;

import classman.render.SceneColor;
import classman.render.StereotypeLook;
import classman.render.StereotypeResolver;
import classman.render.VectorImage;
import specman.metatag.generic.config.MetaTagConfig;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import static specman.Specman.editor;

/** Tells Classman what the stereotypes look like which a diagram refers to by name: the meta tag configuration of that
 * name gives the label and the icon. The icon is parsed once for a configuration and kept as long as the
 * configuration lives, which is until it is edited. */
final class ClassmanStereotypes implements StereotypeResolver {
  private static final Map<MetaTagConfig, StereotypeLook> LOOKS = Collections.synchronizedMap(new WeakHashMap<>());

  @Override
  public StereotypeLook lookOf(String name) {
    MetaTagConfig config = editor().findMetaTagConfig(name);
    return config != null ? LOOKS.computeIfAbsent(config, ClassmanStereotypes::lookOf) : null;
  }

  private static StereotypeLook lookOf(MetaTagConfig config) {
    return new StereotypeLook(config.getLabel(), config.hasIcon() ? iconOf(config) : null)
      .withColors(colorOf(config.getBackgroundColor()), colorOf(config.getBorderColor()),
        colorOf(config.getForegroundColor()));
  }

  private static String colorOf(Color color) {
    return color != null ? SceneColor.of(color) : null;
  }

  /** The SVG of a configuration was checked when it was chosen, but a model file can hold anything: a graphic which
   * can't be shown is no icon. */
  private static VectorImage iconOf(MetaTagConfig config) {
    try {
      return VectorImage.of(config.getIconSvg());
    }
    catch (IllegalArgumentException notDisplayable) {
      return null;
    }
  }
}
