package specman.metatag.generic.config;

import specman.metatag.generic.GenericMetaTag;

import java.awt.Color;
import java.util.Objects;

/**
 * Immutable description of one kind of {@link GenericMetaTag}. The name identifies the
 * configuration (tags refer to it by name) and the icon is held as SVG markup, so the
 * configuration doesn't depend on files outside of the model. Editing a configuration means
 * replacing it by a new instance in the {@link MetaTagConfigRegistry}.
 */
public class MetaTagConfig {
  private final String name;
  private final String label;
  private final String iconSvg;
  private final Color backgroundColor;
  private final Color borderColor;

  public MetaTagConfig(String name, String label, String iconSvg, Color backgroundColor, Color borderColor) {
    this.name = Objects.requireNonNull(name);
    this.label = Objects.requireNonNull(label);
    this.iconSvg = Objects.requireNonNull(iconSvg);
    this.backgroundColor = Objects.requireNonNull(backgroundColor);
    this.borderColor = Objects.requireNonNull(borderColor);
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public String getIconSvg() {
    return iconSvg;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public Color getBorderColor() {
    return borderColor;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MetaTagConfig)) {
      return false;
    }
    MetaTagConfig that = (MetaTagConfig) other;
    return name.equals(that.name) && label.equals(that.label) && iconSvg.equals(that.iconSvg)
      && backgroundColor.equals(that.backgroundColor) && borderColor.equals(that.borderColor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, iconSvg, backgroundColor, borderColor);
  }
}
