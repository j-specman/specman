package classman.render;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the lines of the attributes of a class are, relative to the top left corner of its box, which the user can
 * double click to change them. The line of an attribute which wraps covers all its rows. A class without attributes
 * has one blank line, which stands for an attribute which doesn't exist yet: its key is null.
 */
public class AttributeLineAreas {
    public static final AttributeLineAreas NONE = new AttributeLineAreas(Map.of());

    private final Map<AttributeModel_V001, Rectangle2D.Double> lines;

    /** @param lines the attributes in the order they are shown, the key null stands for the blank line */
    public AttributeLineAreas(Map<AttributeModel_V001, Rectangle2D.Double> lines) {
        this.lines = Collections.unmodifiableMap(new LinkedHashMap<>(lines));
    }

    /** The text areas of the lines, moved so that the top left corner of the box is at the given point. */
    public List<SceneTextArea> textAreasAt(ClassModel_V001 owner, double x, double y) {
        List<SceneTextArea> areas = new ArrayList<>();
        for (Map.Entry<AttributeModel_V001, Rectangle2D.Double> line : lines.entrySet()) {
            Rectangle2D.Double area = line.getValue();
            areas.add(new SceneTextArea(owner, TextKind.ATTRIBUTE_LINE,
                new Rectangle2D.Double(area.x + x, area.y + y, area.width, area.height), line.getKey()));
        }
        return areas;
    }
}
