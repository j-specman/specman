package classman.render;

import java.util.List;

/** A class or note box which is laid out: its size, and its parts relative to its top left corner. */
public class BoxLayout {
    public final double width;
    public final double height;
    private final List<AbstractSceneElement> elements;

    public BoxLayout(double width, double height, List<AbstractSceneElement> elements) {
        this.width = width;
        this.height = height;
        this.elements = List.copyOf(elements);
    }

    /** The parts of the box, moved so that its top left corner is at the given point. */
    public List<AbstractSceneElement> placedAt(double x, double y) {
        return elements.stream().map(element -> element.translated(x, y)).toList();
    }
}
