package classman.model.v001;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** What a class, note or relation looks like beyond its content. Every part which is null has the standard look of
 * the element. Which parts an element can have is told by {@link StyleProperty_V001}. */
public class ElementStyleModel_V001 {
    /** The font sizes which an element can have. */
    public static final List<Integer> FONT_SIZES = List.of(8, 9, 10, 11, 12, 14, 16, 18, 20);

    /** The background color as #rrggbb. */
    public String fill;
    /** The color of all texts of the element as #rrggbb. */
    public String textColor;
    /** The color of the lines of the element as #rrggbb: the border of a box and the line above the attributes of a
     * class, the fold of a note, the line and the marks at the ends of a relation. */
    public String lineColor;
    public FontFamily_V001 font;
    /** One of {@link #FONT_SIZES}. */
    public Integer fontSize;

    /** @return true if the box has the standard look */
    public boolean isStandard() {
        return fill == null && textColor == null && lineColor == null && font == null && fontSize == null;
    }

    public ElementStyleModel_V001 copy() {
        ElementStyleModel_V001 copy = new ElementStyleModel_V001();
        copy.fill = fill;
        copy.textColor = textColor;
        copy.lineColor = lineColor;
        copy.font = font;
        copy.fontSize = fontSize;
        return copy;
    }

    /** @return a copy of this style in which the given parts are those of the source style */
    public ElementStyleModel_V001 with(ElementStyleModel_V001 source, Set<StyleProperty_V001> properties) {
        ElementStyleModel_V001 copy = copy();
        if (properties.contains(StyleProperty_V001.FILL)) {
            copy.fill = source.fill;
        }
        if (properties.contains(StyleProperty_V001.TEXT_COLOR)) {
            copy.textColor = source.textColor;
        }
        if (properties.contains(StyleProperty_V001.LINE_COLOR)) {
            copy.lineColor = source.lineColor;
        }
        if (properties.contains(StyleProperty_V001.FONT)) {
            copy.font = source.font;
        }
        if (properties.contains(StyleProperty_V001.FONT_SIZE)) {
            copy.fontSize = source.fontSize;
        }
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ElementStyleModel_V001)) {
            return false;
        }
        ElementStyleModel_V001 style = (ElementStyleModel_V001) other;
        return Objects.equals(fill, style.fill) && Objects.equals(textColor, style.textColor)
            && Objects.equals(lineColor, style.lineColor) && font == style.font
            && Objects.equals(fontSize, style.fontSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fill, textColor, lineColor, font, fontSize);
    }
}
