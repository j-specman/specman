package classman.render;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.FontFamily_V001;

import java.awt.Font;
import java.util.Objects;

/** A font family with a size, the font of all texts in a class box. */
public final class TextFont {
    public static final TextFont DEFAULT = new TextFont(FontFamily_V001.ROBOTO, DiagramStyle.FONT_SIZE);

    public final FontFamily_V001 family;
    public final int size;
    private Font plain;
    private Font bold;

    public TextFont(FontFamily_V001 family, int size) {
        this.family = family;
        this.size = size;
    }

    /** @return the font which the style asks for, parts which it leaves out are the default ones */
    public static TextFont of(ElementStyleModel_V001 style) {
        return new TextFont(style.font != null ? style.font : DEFAULT.family,
            style.fontSize != null ? style.fontSize : DEFAULT.size);
    }

    /** @return the Java2D font, which is made once */
    public synchronized Font awtFont(boolean isBold) {
        if (plain == null) {
            plain = DiagramFonts.plain(family, size);
            bold = DiagramFonts.bold(family, size);
        }
        return isBold ? bold : plain;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TextFont font && font.family == family && font.size == size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, size);
    }
}
