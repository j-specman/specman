package classman.render;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the rows of the tagged values of a class are, relative to the top left corner of its box, which the user
 * can double click to change them. A tagged value which a change set has removed has no row to change.
 */
public class TaggedValueAreas {
    public static final TaggedValueAreas NONE = new TaggedValueAreas(Map.of());

    private final Map<TaggedValueModel_V001, Rectangle2D.Double> rows;

    /** @param rows the tagged values in the order they are shown */
    public TaggedValueAreas(Map<TaggedValueModel_V001, Rectangle2D.Double> rows) {
        this.rows = Collections.unmodifiableMap(new LinkedHashMap<>(rows));
    }

    /** The text areas of the rows, moved so that the top left corner of the box is at the given point. */
    public List<SceneTextArea> textAreasAt(ClassModel_V001 owner, double x, double y) {
        List<SceneTextArea> areas = new ArrayList<>();
        for (Map.Entry<TaggedValueModel_V001, Rectangle2D.Double> row : rows.entrySet()) {
            Rectangle2D.Double area = row.getValue();
            areas.add(new SceneTextArea(owner, TextKind.TAGGED_VALUE,
                new Rectangle2D.Double(area.x + x, area.y + y, area.width, area.height), row.getKey()));
        }
        return areas;
    }
}
