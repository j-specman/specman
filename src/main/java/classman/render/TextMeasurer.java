package classman.render;

/** Measures text for the layout. The real implementation uses the bundled fonts, tests use fixed widths. */
public interface TextMeasurer {

    /** The width of the text in one line, in pixels. */
    double width(String text, boolean bold);

    /** The height of one line, including the space between two lines. */
    double lineHeight();

    /** The distance from the top of a line to its baseline. */
    double ascent();
}
