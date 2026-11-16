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

    public SceneText(double x, double y, String text, boolean bold, TextAnchor anchor, double width) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.bold = bold;
        this.anchor = anchor;
        this.width = width;
    }

    @Override
    public SceneText translated(double dx, double dy) {
        return new SceneText(x + dx, y + dy, text, bold, anchor, width);
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
