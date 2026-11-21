package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** A laid out relation: its parts, the area they cover, and the point where notes can be attached to it. */
public class RelationLayout {
    /** The change marks which go below the boxes of the diagram, like the thick line of an added relation. */
    public final List<AbstractSceneElement> underlay;
    public final List<AbstractSceneElement> elements;
    public final Rectangle2D.Double bounds;
    /** Null for the layout of an association class, which has nothing to attach to. */
    public final Point2D.Double attachPoint;
    /** The boxes which belong to the relation: its association class, if it has one. */
    public final List<SceneElementArea> elementAreas;
    /** The texts of the boxes which belong to the relation, which the user can change in place. */
    public final List<SceneTextArea> textAreas;

    public RelationLayout(List<AbstractSceneElement> elements, Rectangle2D.Double bounds,
                          List<SceneElementArea> elementAreas, List<SceneTextArea> textAreas) {
        this(List.of(), elements, bounds, null, elementAreas, textAreas);
    }

    public RelationLayout(List<AbstractSceneElement> underlay, List<AbstractSceneElement> elements,
                          Rectangle2D.Double bounds, Point2D.Double attachPoint, List<SceneElementArea> elementAreas,
                          List<SceneTextArea> textAreas) {
        this.underlay = List.copyOf(underlay);
        this.elements = List.copyOf(elements);
        this.bounds = bounds;
        this.attachPoint = attachPoint;
        this.elementAreas = List.copyOf(elementAreas);
        this.textAreas = List.copyOf(textAreas);
    }
}
