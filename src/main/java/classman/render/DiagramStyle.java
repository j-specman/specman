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
    /** The space between the text of a note and its right border: the fold plus the usual padding, so the text keeps clear of the fold. */
    public static final double NOTE_RIGHT_PADDING = 16;
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
    public static final double MULTIPLICITY_SIDE_GAP = 8;
    public static final double MULTIPLICITY_ALONG_GAP = 4;
    public static final double MULTIPLICITY_BORDER_GAP = 2;
    public static final double LABEL_GAP = 6;
    /** The space between the text of an attribute and the end of a note line which is attached to it. */
    public static final double ATTRIBUTE_ATTACH_GAP = 2;
    /** The distance between the line of a relation and its association class. */
    public static final double ASSOC_CLASS_GAP = 30;
    /** How far the loop of a relation from a class to itself reaches out of the class. */
    public static final double LOOP_SIZE = 20;

    /** The shipped font. A viewer without it falls back to the following families. */
    public static final String FONT_FAMILY = "Roboto";
    public static final String SVG_FONT_FAMILIES = FONT_FAMILY + ", Arial, sans-serif";
    public static final int FONT_SIZE = 12;
    /** The empty space around the scene in an SVG. */
    public static final double SVG_MARGIN = 10;

    public static final String CLASS_FILL = "#f2e9c4";
    public static final String ASSOC_CLASS_FILL = "#e0e0e0";
    public static final String NOTE_FILL = "#c0e8f9";
    public static final String STROKE = "#000000";
    public static final String WHITE = "#ffffff";
    public static final double STROKE_WIDTH = 1;

    // ---- change marks ----

    /** The width of the stroke which makes the frame of an added or removed class or note, half of it shows outside. */
    public static final double FRAME_STROKE_WIDTH = 12;
    /** The width of the line which is drawn under an added or removed relation. */
    public static final double RELATION_HIGHLIGHT_WIDTH = 9;
    /** How far the highlight of a changed text reaches out of the text to the left and right. */
    public static final double HIGHLIGHT_MARGIN = 2;
    /** The share of white in the pale variant of a change color. */
    public static final double PALE_SHARE = 0.6;
    /** The share of white which veils the fill of a removed class or note. */
    public static final double VEIL_SHARE = 0.5;
    public static final String VEILED_TEXT = "#9a9a9a";
    public static final String VEILED_STROKE = "#a0a0a0";
    /** The line of a removed relation: darker than the border of a removed box, because it lies on its colored band. */
    public static final String VEILED_LINE = "#7a7a7a";
    public static final String STRIKE_COLOR = "#444444";
    public static final double STRIKE_WIDTH = 1.2;
    /** Where the strike through line stands: this part of the ascent above the baseline. */
    public static final double STRIKE_HEIGHT_PART = 0.35;
    /** Half the size of the cross on a removed relation. */
    public static final double CROSS_HALF_SIZE = 5;
    /** The color of a change set which the host application doesn't know. */
    public static final String UNKNOWN_CHANGE_COLOR = "#c8c8c8";

    private DiagramStyle() {
    }
}
