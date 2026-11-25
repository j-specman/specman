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
  private final Color foregroundColor;
  private final boolean freetextAllowed;

  /** @param iconSvg SVG markup of the icon, null for a tag without icon. A tag needs a label or
   * an icon, but that is a matter of the configuration dialog, not enforced here.
   * @param backgroundColor null for the default of whoever shows the configuration
   * @param borderColor null for the default, which is no border for a tag
   * @param foregroundColor the text color, null for the default of whoever shows the configuration
   * @param freetextAllowed whether the user can attach a freetext to tags of this configuration */
  public MetaTagConfig(String name, String label, String iconSvg, Color backgroundColor, Color borderColor,
                       Color foregroundColor, boolean freetextAllowed) {
    this.name = Objects.requireNonNull(name);
    this.label = Objects.requireNonNull(label);
    this.iconSvg = iconSvg;
    this.backgroundColor = backgroundColor;
    this.borderColor = borderColor;
    this.foregroundColor = foregroundColor;
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

  /** @return the background color, null for the default */
  public Color getBackgroundColor() {
    return backgroundColor;
  }

  /** @return the border color, null for the default */
  public Color getBorderColor() {
    return borderColor;
  }

  /** @return the text color, null for the default */
  public Color getForegroundColor() {
    return foregroundColor;
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
      && Objects.equals(backgroundColor, that.backgroundColor) && Objects.equals(borderColor, that.borderColor)
      && Objects.equals(foregroundColor, that.foregroundColor) && freetextAllowed == that.freetextAllowed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, iconSvg, backgroundColor, borderColor, foregroundColor, freetextAllowed);
  }
}
