package classman.render;

import classman.model.v001.ElementStyleModel_V001;

import static classman.render.DiagramStyle.VEIL_SHARE;
import static classman.render.DiagramStyle.WHITE;

/** The colors of an element from its style, falling back to the standard ones of its kind, and the veiled variants of
 * an element which a change set has removed. An own color is veiled by mixing it with white, a standard one has a
 * veiled color of its own. */
final class StyleColors {
    private final ElementStyleModel_V001 style;
    private final String standardFill;
    private final String standardLine;
    private final String veiledStandardLine;

    /** @param standardFill null for an element without a background
     * @param veiledStandardLine the line color of a removed element which has no line color of its own */
    StyleColors(ElementStyleModel_V001 style, String standardFill, String standardLine, String veiledStandardLine) {
        this.style = style;
        this.standardFill = standardFill;
        this.standardLine = standardLine;
        this.veiledStandardLine = veiledStandardLine;
    }

    /** @return the background, null if the element has none */
    String fill(boolean isRemoved) {
        String fill = style.fill != null ? style.fill : standardFill;
        return isRemoved && fill != null ? veiled(fill) : fill;
    }

    String line(boolean isRemoved) {
        if (!isRemoved) {
            return style.lineColor != null ? style.lineColor : standardLine;
        }
        return style.lineColor != null ? veiled(style.lineColor) : veiledStandardLine;
    }

    /** @return the color of a text, null for the default one
     * @param veiledStandardText the color of the text of a removed element which has no text color of its own */
    String text(boolean isRemoved, String veiledStandardText) {
        if (!isRemoved) {
            return style.textColor;
        }
        return style.textColor != null ? veiled(style.textColor) : veiledStandardText;
    }

    /** @return the color of a text of a removed element which lies on the strong color of its change set, where a veil
     * would hardly show: the own text color as it is, or the given one
     * @param standardMarkedText the color without a text color of its own */
    String markedText(boolean isRemoved, String standardMarkedText) {
        if (!isRemoved) {
            return style.textColor;
        }
        return style.textColor != null ? style.textColor : standardMarkedText;
    }

    private static String veiled(String color) {
        return ColorMix.blend(color, WHITE, VEIL_SHARE);
    }
}
