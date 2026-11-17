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

    public SceneRectangle(double x, double y, double width, double height, String fill, String stroke) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fill = fill;
        this.stroke = stroke;
    }

    @Override
    public SceneRectangle translated(double dx, double dy) {
        return new SceneRectangle(x + dx, y + dy, width, height, fill, stroke);
    }
}
