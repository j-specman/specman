package classman.render;

import java.util.List;

/** Everything which is drawn for a diagram, and the area it covers. */
public class Scene {
    public final List<AbstractSceneElement> elements;
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Scene(List<AbstractSceneElement> elements, double x, double y, double width, double height) {
        this.elements = List.copyOf(elements);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
