package classman.render;

import classman.model.v001.FontFamily_V001;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/** The fonts which Specman ships, loaded from the class path and registered so that Java2D and the SVG renderers find
 * them by name. Roboto is the font of a diagram unless a class asks for another. */
public final class DiagramFonts {
    private static final Map<FontFamily_V001, Font> PLAIN = new EnumMap<>(FontFamily_V001.class);
    private static final Map<FontFamily_V001, Font> BOLD = new EnumMap<>(FontFamily_V001.class);

    static {
        load(FontFamily_V001.SITKA, "fonts/sitka/SitkaDisplay.ttf", "fonts/sitka/SitkaDisplay-Bold.ttf");
        load(FontFamily_V001.ROBOTO, "fonts/roboto/Roboto-Regular.ttf", "fonts/roboto/Roboto-Bold.ttf");
        load(FontFamily_V001.COURIER, "fonts/courierprime/CourierPrime.ttf", "fonts/courierprime/CourierPrime-Bold.ttf");
    }

    private DiagramFonts() {
    }

    public static Font plain(float size) {
        return plain(TextFont.DEFAULT.family, size);
    }

    public static Font bold(float size) {
        return bold(TextFont.DEFAULT.family, size);
    }

    public static Font plain(FontFamily_V001 family, float size) {
        return PLAIN.get(family).deriveFont(size);
    }

    public static Font bold(FontFamily_V001 family, float size) {
        return BOLD.get(family).deriveFont(size);
    }

    private static void load(FontFamily_V001 family, String plainResource, String boldResource) {
        PLAIN.put(family, load(plainResource));
        BOLD.put(family, load(boldResource));
    }

    private static Font load(String resource) {
        try (InputStream stream = DiagramFonts.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("The font file " + resource + " is missing");
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (FontFormatException | IOException e) {
            throw new IllegalStateException("The font file " + resource + " can't be loaded", e);
        }
    }
}
