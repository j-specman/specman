package classman.render;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/** Measures text with Java2D, with the shipped font of the diagram. */
public class AwtTextMeasurer implements TextMeasurer {
    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private final Font plain = DiagramFonts.plain(DiagramStyle.FONT_SIZE);
    private final Font bold = DiagramFonts.bold(DiagramStyle.FONT_SIZE);
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
