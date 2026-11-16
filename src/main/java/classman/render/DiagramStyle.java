package classman.render;

/** The measures and colors of the diagram. Fonts are the business of the TextMeasurer. */
public final class DiagramStyle {
    public static final double PADDING_X = 6;
    public static final double PADDING_Y = 4;
    /** Indent of the following lines of an attribute which is wrapped. */
    public static final double CONTINUATION_INDENT = 12;
    /** The space between two classes or notes which the layout stacks. */
    public static final double STACK_GAP = 40;
    public static final double MIN_BOX_WIDTH = 40;
    /** The size of the folded corner of a note. */
    public static final double NOTE_FOLD = 10;
    /** A note without width wraps its text when it would be wider than this. */
    public static final double NOTE_MAX_AUTO_WIDTH = 240;

    public static final String CLASS_FILL = "#fdd34d";
    public static final String NOTE_FILL = "#6fcdf2";
    public static final String STROKE = "#000000";

    private DiagramStyle() {
    }
}
