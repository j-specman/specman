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
  private final boolean freetextAllowed;

  /** @param iconSvg SVG markup of the icon, null for a tag without icon. A tag needs a label or
   * an icon, but that is a matter of the configuration dialog, not enforced here.
   * @param borderColor null for a tag without border
   * @param freetextAllowed whether the user can attach a freetext to tags of this configuration */
  public MetaTagConfig(String name, String label, String iconSvg, Color backgroundColor, Color borderColor,
                       boolean freetextAllowed) {
    this.name = Objects.requireNonNull(name);
    this.label = Objects.requireNonNull(label);
    this.iconSvg = iconSvg;
    this.backgroundColor = Objects.requireNonNull(backgroundColor);
    this.borderColor = borderColor;
    this.freetextAllowed = freetextAllowed;
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

  public boolean hasIcon() {
    return iconSvg != null;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public boolean hasBorderColor() {
    return borderColor != null;
  }

  /** @return the border color, null if the tag has no border */
  public Color getBorderColor() {
    return borderColor;
  }

  public boolean allowsFreetext() {
    return freetextAllowed;
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
    return name.equals(that.name) && label.equals(that.label) && Objects.equals(iconSvg, that.iconSvg)
      && backgroundColor.equals(that.backgroundColor) && Objects.equals(borderColor, that.borderColor)
      && freetextAllowed == that.freetextAllowed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, iconSvg, backgroundColor, borderColor, freetextAllowed);
  }
}
