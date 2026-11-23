package classman.model.v001;

import java.util.List;
import java.util.Objects;

/** What a class box looks like beyond its content. Every part which is null has the standard look of classes. */
public class ClassStyleModel_V001 {
    /** The font sizes which a box can have. */
    public static final List<Integer> FONT_SIZES = List.of(8, 9, 10, 11, 12, 14, 16, 18, 20);

    /** The background color of the box as #rrggbb. */
    public String fill;
    /** The color of all texts of the box as #rrggbb. */
    public String textColor;
    /** The color of the border of the box and of the line above its attributes as #rrggbb. */
    public String lineColor;
    public FontFamily_V001 font;
    /** One of {@link #FONT_SIZES}. */
    public Integer fontSize;

    /** @return true if the box has the standard look */
    public boolean isStandard() {
        return fill == null && textColor == null && lineColor == null && font == null && fontSize == null;
    }

    public ClassStyleModel_V001 copy() {
        ClassStyleModel_V001 copy = new ClassStyleModel_V001();
        copy.fill = fill;
        copy.textColor = textColor;
        copy.lineColor = lineColor;
        copy.font = font;
        copy.fontSize = fontSize;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ClassStyleModel_V001)) {
            return false;
        }
        ClassStyleModel_V001 style = (ClassStyleModel_V001) other;
        return Objects.equals(fill, style.fill) && Objects.equals(textColor, style.textColor)
            && Objects.equals(lineColor, style.lineColor) && font == style.font
            && Objects.equals(fontSize, style.fontSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fill, textColor, lineColor, font, fontSize);
    }
}
