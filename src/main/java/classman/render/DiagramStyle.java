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

    /** Length and half width of the marks at the ends of a relation. */
    public static final double DIAMOND_LENGTH = 14;
    public static final double DIAMOND_HALF_WIDTH = 4;
    public static final double ARROW_LENGTH = 12;
    public static final double ARROW_HALF_WIDTH = 5;
    public static final double TRIANGLE_LENGTH = 14;
    public static final double TRIANGLE_HALF_WIDTH = 7;
    /** The distances of the texts of a relation from its line. */
    public static final double MULTIPLICITY_SIDE_GAP = 6;
    public static final double MULTIPLICITY_ALONG_GAP = 4;
    public static final double MULTIPLICITY_BORDER_GAP = 2;
    public static final double LABEL_GAP = 6;
    /** The distance between the line of a relation and its association class. */
    public static final double ASSOC_CLASS_GAP = 30;
    /** How far the loop of a relation from a class to itself reaches out of the class. */
    public static final double LOOP_SIZE = 20;

    /** Placeholder until the bundled fonts are chosen. */
    public static final String FONT_FAMILY = "Arial";
    public static final int FONT_SIZE = 11;
    /** The empty space around the scene in an SVG. */
    public static final double SVG_MARGIN = 10;

    public static final String CLASS_FILL = "#fdd34d";
    public static final String ASSOC_CLASS_FILL = "#e0e0e0";
    public static final String NOTE_FILL = "#6fcdf2";
    public static final String STROKE = "#000000";
    public static final String WHITE = "#ffffff";

    private DiagramStyle() {
    }
}
