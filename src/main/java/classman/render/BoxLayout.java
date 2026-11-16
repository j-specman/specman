package classman.render;

import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A class or note box which is laid out: its size, and its parts relative to its top left corner. */
public class BoxLayout {
    public final double width;
    public final double height;
    private final List<AbstractSceneElement> elements;
    private final Map<String, Rectangle2D.Double> attributeAreas;

    public BoxLayout(double width, double height, List<AbstractSceneElement> elements) {
        this(width, height, elements, Map.of());
    }

    /** @param attributeAreas the lines of the attributes which have an ID, relative to the top left corner of the box */
    public BoxLayout(double width, double height, List<AbstractSceneElement> elements,
                     Map<String, Rectangle2D.Double> attributeAreas) {
        this.width = width;
        this.height = height;
        this.elements = List.copyOf(elements);
        this.attributeAreas = Map.copyOf(attributeAreas);
    }

    /** The parts of the box, moved so that its top left corner is at the given point. */
    public List<AbstractSceneElement> placedAt(double x, double y) {
        return elements.stream().map(element -> element.translated(x, y)).toList();
    }

    /** The lines of the attributes which have an ID, moved so that the top left corner of the box is at the given point. */
    public Map<String, Rectangle2D.Double> attributeAreasAt(double x, double y) {
        Map<String, Rectangle2D.Double> placed = new LinkedHashMap<>();
        attributeAreas.forEach((id, area) ->
            placed.put(id, new Rectangle2D.Double(area.x + x, area.y + y, area.width, area.height)));
        return placed;
    }
}
