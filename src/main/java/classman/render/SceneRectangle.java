package classman.render;

public class SceneRectangle extends AbstractSceneElement {
    public final double x;
    public final double y;
    public final double width;
    public final double height;
    /** A color like #f2e9c4, or null for no fill. */
    public final String fill;
    /** A color like #000000, or null for no outline. */
    public final String stroke;
    public final double strokeWidth;
    /** See {@link #keepingVisible()}. */
    public final boolean keepsVisible;

    public SceneRectangle(double x, double y, double width, double height, String fill, String stroke) {
        this(x, y, width, height, fill, stroke, DiagramStyle.STROKE_WIDTH);
    }

    public SceneRectangle(double x, double y, double width, double height, String fill, String stroke,
                          double strokeWidth) {
        this(x, y, width, height, fill, stroke, strokeWidth, false);
    }

    private SceneRectangle(double x, double y, double width, double height, String fill, String stroke,
                           double strokeWidth, boolean keepsVisible) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fill = fill;
        this.stroke = stroke;
        this.strokeWidth = strokeWidth;
        this.keepsVisible = keepsVisible;
    }

    /** @return the same rectangle which painters keep recognizable on the screen, however small the diagram is painted:
     * the outline isn't thinner than {@link DiagramStyle#MIN_VISIBLE_STROKE}, and a filled rectangle isn't smaller than
     * {@link DiagramStyle#MIN_VISIBLE_HIGHLIGHT} in height and width */
    public SceneRectangle keepingVisible() {
        return new SceneRectangle(x, y, width, height, fill, stroke, strokeWidth, true);
    }

    @Override
    public SceneRectangle translated(double dx, double dy) {
        return new SceneRectangle(x + dx, y + dy, width, height, fill, stroke, strokeWidth, keepsVisible);
    }
}
