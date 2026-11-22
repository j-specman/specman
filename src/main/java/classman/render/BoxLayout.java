package classman.render;

import classman.model.v001.ClassModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A class or note box which is laid out: its size, and its parts relative to its top left corner. */
public class BoxLayout {
    public final double width;
    public final double height;
    /** How far the parts of the box, like the frame of an added class, reach out of it on every side. */
    public final double outset;
    private final List<AbstractSceneElement> elements;
    private final Map<String, Rectangle2D.Double> attributeAreas;
    private final Map<TextKind, Rectangle2D.Double> textAreas;
    private final AttributeLineAreas attributeLines;
    private final Map<TextKind, Rectangle2D.Double> placeholderAreas;

    public BoxLayout(double width, double height, List<AbstractSceneElement> elements) {
        this(width, height, elements, Map.of(), 0, Map.of());
    }

    /**
     * @param attributeAreas the lines of the attributes which have an ID, relative to the top left corner of the box
     * @param outset how far the parts reach out of the box
     * @param textAreas where the texts are which the user can change in place, relative to the top left corner
     */
    public BoxLayout(double width, double height, List<AbstractSceneElement> elements,
                     Map<String, Rectangle2D.Double> attributeAreas, double outset,
                     Map<TextKind, Rectangle2D.Double> textAreas) {
        this(width, height, elements, attributeAreas, outset, textAreas, AttributeLineAreas.NONE);
    }

    /**
     * @param attributeLines where the lines of the attributes are, which the user can change
     */
    public BoxLayout(double width, double height, List<AbstractSceneElement> elements,
                     Map<String, Rectangle2D.Double> attributeAreas, double outset,
                     Map<TextKind, Rectangle2D.Double> textAreas, AttributeLineAreas attributeLines) {
        this(width, height, elements, attributeAreas, outset, textAreas, attributeLines, Map.of());
    }

    /**
     * @param placeholderAreas where the texts would stand which the box doesn't have yet, relative to the top left
     * corner. They may lie outside the box.
     */
    public BoxLayout(double width, double height, List<AbstractSceneElement> elements,
                     Map<String, Rectangle2D.Double> attributeAreas, double outset,
                     Map<TextKind, Rectangle2D.Double> textAreas, AttributeLineAreas attributeLines,
                     Map<TextKind, Rectangle2D.Double> placeholderAreas) {
        this.width = width;
        this.height = height;
        this.outset = outset;
        this.elements = List.copyOf(elements);
        this.attributeAreas = Map.copyOf(attributeAreas);
        this.textAreas = Map.copyOf(textAreas);
        this.attributeLines = attributeLines;
        this.placeholderAreas = Map.copyOf(placeholderAreas);
    }

    /** The areas of the attribute lines of the class, moved so that the top left corner of the box is at the given point. */
    public List<SceneTextArea> attributeTextAreasAt(ClassModel_V001 owner, double x, double y) {
        return attributeLines.textAreasAt(owner, x, y);
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

    /** The areas of the texts the user can change, moved so that the top left corner of the box is at the given point. */
    public Map<TextKind, Rectangle2D.Double> textAreasAt(double x, double y) {
        return moved(textAreas, x, y);
    }

    private static Map<TextKind, Rectangle2D.Double> moved(Map<TextKind, Rectangle2D.Double> areas, double x, double y) {
        Map<TextKind, Rectangle2D.Double> placed = new LinkedHashMap<>();
        areas.forEach((kind, area) ->
            placed.put(kind, new Rectangle2D.Double(area.x + x, area.y + y, area.width, area.height)));
        return placed;
    }

    /** The areas where the texts would stand which the box doesn't have yet, moved so that the top left corner of the
     * box is at the given point. */
    public Map<TextKind, Rectangle2D.Double> placeholderAreasAt(double x, double y) {
        return moved(placeholderAreas, x, y);
    }

    /** The area of the box at the given point including the parts which reach out of it. */
    public Rectangle2D.Double coveredAt(double x, double y) {
        return new Rectangle2D.Double(x - outset, y - outset, width + 2 * outset, height + 2 * outset);
    }
}
