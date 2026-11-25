package specman.model.v002;

import specman.metatag.generic.config.MetaTagConfig;

import java.awt.Color;

/** Model of a {@link MetaTagConfig}. Colors are held as ARGB values, so they can be transparent.
 * A color which is null is the default of whoever uses the configuration. */
public class MetaTagConfigModel_V002 {
    public final String name;
    /** Empty for a tag without label. */
    public final String label;
    /** SVG markup, null for a tag without icon. */
    public final String iconSvg;
    public final Integer backgroundColor;
    public final Integer borderColor;
    public final Integer foregroundColor;
    public final boolean freetextAllowed;

    public MetaTagConfigModel_V002(String name, String label, String iconSvg, Integer backgroundColor, Integer borderColor,
                                   Integer foregroundColor, boolean freetextAllowed) {
        this.name = name;
        this.label = label;
        this.iconSvg = iconSvg;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.foregroundColor = foregroundColor;
        this.freetextAllowed = freetextAllowed;
    }

    public static MetaTagConfigModel_V002 from(MetaTagConfig config) {
        return new MetaTagConfigModel_V002(
            config.getName(),
            config.getLabel(),
            config.getIconSvg(),
            toArgb(config.getBackgroundColor()),
            toArgb(config.getBorderColor()),
            toArgb(config.getForegroundColor()),
            config.allowsFreetext());
    }

    public MetaTagConfig toConfig() {
        return new MetaTagConfig(name, label, iconSvg, toColor(backgroundColor), toColor(borderColor), toColor(foregroundColor), freetextAllowed);
    }

    private static Integer toArgb(Color color) {
        return color != null ? color.getRGB() : null;
    }

    private static Color toColor(Integer argb) {
        return argb != null ? new Color(argb, true) : null;
    }
}
