package classman.render;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/** Measures text with Java2D, with the font of the diagram. */
public class AwtTextMeasurer implements TextMeasurer {
    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private final Font plain = new Font(DiagramStyle.FONT_FAMILY, Font.PLAIN, DiagramStyle.FONT_SIZE);
    private final Font bold = plain.deriveFont(Font.BOLD);
    private final LineMetrics lineMetrics = plain.getLineMetrics("Ag", CONTEXT);

    @Override
    public double width(String text, boolean isBold) {
        return (isBold ? bold : plain).getStringBounds(text, CONTEXT).getWidth();
    }

    @Override
    public double lineHeight() {
        return lineMetrics.getHeight();
    }

    @Override
    public double ascent() {
        return lineMetrics.getAscent();
    }
}
