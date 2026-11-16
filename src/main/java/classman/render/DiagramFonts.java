package classman.render;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/** The shipped Roboto font, loaded from the class path and registered so that Java2D and the SVG renderers find it by name. */
public final class DiagramFonts {
    private static final Font PLAIN = load("fonts/roboto/Roboto-Regular.ttf");
    private static final Font BOLD = load("fonts/roboto/Roboto-Bold.ttf");

    private DiagramFonts() {
    }

    public static Font plain(float size) {
        return PLAIN.deriveFont(size);
    }

    public static Font bold(float size) {
        return BOLD.deriveFont(size);
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
