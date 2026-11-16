package classman.render;

/** One line of text. */
public class SceneText extends AbstractSceneElement {
    public final double x;
    /** The y of the baseline. */
    public final double y;
    public final String text;
    public final boolean bold;
    /** Says what x is: the start of the text, or its middle. */
    public final TextAnchor anchor;

    public SceneText(double x, double y, String text, boolean bold, TextAnchor anchor) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.bold = bold;
        this.anchor = anchor;
    }

    @Override
    public SceneText translated(double dx, double dy) {
        return new SceneText(x + dx, y + dy, text, bold, anchor);
    }
}
