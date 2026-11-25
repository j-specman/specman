package classman.render;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** The colors of a diagram are strings: #rrggbb, or #rrggbbaa for a color which is not opaque. This is the one place
 * which knows how to read and write them, for the painters and for whoever shows a color of a diagram. */
public final class SceneColor {
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");
    private static final int RGB_LENGTH = "#rrggbb".length();
    private static final int OPAQUE = 255;

    private SceneColor() {
    }

    /** @return true if the text is a color, #rrggbb or #rrggbbaa */
    public static boolean isValid(String color) {
        return COLOR.matcher(color).matches();
    }

    public static Color parse(String color) {
        Color rgb = Color.decode(rgb(color));
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha(color));
    }

    /** @return the color without alpha, #rrggbb */
    public static String rgb(String color) {
        return color.length() > RGB_LENGTH ? color.substring(0, RGB_LENGTH) : color;
    }

    /** @return the opacity between 0 and 1 for the opacity attributes of an SVG, null for an opaque color, which needs none */
    public static String opacity(String color) {
        int alpha = alpha(color);
        if (alpha == OPAQUE) {
            return null;
        }
        return new BigDecimal(alpha).divide(new BigDecimal(OPAQUE), 2, RoundingMode.HALF_UP).stripTrailingZeros()
            .toPlainString();
    }

    /** @return the color as #rrggbb, or #rrggbbaa if it is not opaque */
    public static String of(Color color) {
        String rgb = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return color.getAlpha() == OPAQUE ? rgb : rgb + String.format("%02x", color.getAlpha());
    }

    /** @return the alpha of the color, 255 if it has none */
    static int alpha(String color) {
        return color.length() > RGB_LENGTH ? Integer.parseInt(color.substring(RGB_LENGTH), 16) : OPAQUE;
    }
}
