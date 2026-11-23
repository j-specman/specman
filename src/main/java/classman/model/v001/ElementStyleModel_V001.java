package classman.model.v001;

import java.util.List;
import java.util.Objects;

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
