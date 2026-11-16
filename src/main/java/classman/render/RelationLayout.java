package classman.render;

import java.awt.geom.Rectangle2D;
import java.util.List;

/** A laid out relation: its parts, and the area they cover. */
public class RelationLayout {
    public final List<AbstractSceneElement> elements;
    public final Rectangle2D.Double bounds;

    public RelationLayout(List<AbstractSceneElement> elements, Rectangle2D.Double bounds) {
        this.elements = List.copyOf(elements);
        this.bounds = bounds;
    }
}
