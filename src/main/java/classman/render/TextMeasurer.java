package classman.render;

/** Measures text for the layout. The real implementation uses the bundled fonts, tests use fixed widths. */
public interface TextMeasurer {

    /** The width of the text in one line, in pixels. */
    double width(String text, boolean bold);

    /** The height of one line, including the space between two lines. */
    double lineHeight();

    /** The distance from the top of a line to its baseline. */
    double ascent();

    /** @return a measurer for texts in the font, which measures like this one in every other respect */
    TextMeasurer forFont(TextFont font);
}
