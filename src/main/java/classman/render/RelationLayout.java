package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** A laid out relation: its parts, the area they cover, and the point where notes can be attached to it. */
public class RelationLayout {
    public final List<AbstractSceneElement> elements;
    public final Rectangle2D.Double bounds;
    /** Null for the layout of an association class, which has nothing to attach to. */
    public final Point2D.Double attachPoint;

    public RelationLayout(List<AbstractSceneElement> elements, Rectangle2D.Double bounds) {
        this(elements, bounds, null);
    }

    public RelationLayout(List<AbstractSceneElement> elements, Rectangle2D.Double bounds, Point2D.Double attachPoint) {
        this.elements = List.copyOf(elements);
        this.bounds = bounds;
        this.attachPoint = attachPoint;
    }
}
