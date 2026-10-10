package specman.model.v002;

import specman.metatag.generic.config.MetaTagConfig;

import java.awt.Color;

/** Model of a {@link MetaTagConfig}. Colors are held as RGB values, like the shade of a step. */
public class MetaTagConfigModel_V002 {
    public final String name;
    /** Empty for a tag without label. */
    public final String label;
    /** SVG markup, null for a tag without icon. */
    public final String iconSvg;
    public final int backgroundColor;
    /** Null for a tag without border. */
    public final Integer borderColor;
    public final boolean freetextAllowed;

    public MetaTagConfigModel_V002(String name, String label, String iconSvg, int backgroundColor, Integer borderColor, boolean freetextAllowed) {
        this.name = name;
        this.label = label;
        this.iconSvg = iconSvg;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.freetextAllowed = freetextAllowed;
    }

    public static MetaTagConfigModel_V002 from(MetaTagConfig config) {
        return new MetaTagConfigModel_V002(
            config.getName(),
            config.getLabel(),
            config.getIconSvg(),
            config.getBackgroundColor().getRGB(),
            config.hasBorderColor() ? config.getBorderColor().getRGB() : null,
            config.allowsFreetext());
    }

    public MetaTagConfig toConfig() {
        return new MetaTagConfig(name, label, iconSvg, new Color(backgroundColor), borderColor != null ? new Color(borderColor) : null, freetextAllowed);
    }
}
