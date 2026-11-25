package classman.render;

/** A vector graphic in a rectangle of the diagram, like the icon of a stereotype. */
public class SceneVectorImage extends AbstractSceneElement {
    public final double x;
    public final double y;
    public final double width;
    public final double height;
    public final VectorImage image;

    public SceneVectorImage(double x, double y, double width, double height, VectorImage image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    @Override
    public SceneVectorImage translated(double dx, double dy) {
        return new SceneVectorImage(x + dx, y + dy, width, height, image);
    }
}
