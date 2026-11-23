package classman.render;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/** Measures text with Java2D, with the shipped fonts of the diagram. Without a font it measures the default one. */
public class AwtTextMeasurer implements TextMeasurer {
    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private final Font plain;
    private final Font bold;
    private final LineMetrics lineMetrics;

    public AwtTextMeasurer() {
        this(TextFont.DEFAULT);
    }

    private AwtTextMeasurer(TextFont font) {
        plain = font.awtFont(false);
        bold = font.awtFont(true);
        lineMetrics = plain.getLineMetrics("Ag", CONTEXT);
    }

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

    @Override
    public TextMeasurer forFont(TextFont font) {
        return new AwtTextMeasurer(font);
    }
}
