package classman.render;

/** One line of text. */
public class SceneText extends AbstractSceneElement {
    public final double x;
    /** The y of the baseline. */
    public final double y;
    public final String text;
    public final boolean bold;
    /** Says what x is: the start of the text, its middle, or its end. */
    public final TextAnchor anchor;
    /** The width of the text as the layout measured it. */
    public final double width;
    /** A color like #9a9a9a, or null for the default text color, black. */
    public final String fill;
    /** The font family and size the text is drawn in and was measured with, never null. */
    public final TextFont font;

    public SceneText(double x, double y, String text, boolean bold, TextAnchor anchor, double width) {
        this(x, y, text, bold, anchor, width, null);
    }

    public SceneText(double x, double y, String text, boolean bold, TextAnchor anchor, double width, String fill) {
        this(x, y, text, bold, anchor, width, fill, TextFont.DEFAULT);
    }

    public SceneText(double x, double y, String text, boolean bold, TextAnchor anchor, double width, String fill,
                     TextFont font) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.bold = bold;
        this.anchor = anchor;
        this.width = width;
        this.fill = fill;
        this.font = font;
    }

    @Override
    public SceneText translated(double dx, double dy) {
        return new SceneText(x + dx, y + dy, text, bold, anchor, width, fill, font);
    }

    /** The x of the left end of the text. */
    public double left() {
        switch (anchor) {
            case MIDDLE:
                return x - width / 2;
            case END:
                return x - width;
            default:
                return x;
        }
    }
}
