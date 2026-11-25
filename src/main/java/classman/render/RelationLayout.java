package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** A laid out relation: its parts, the area they cover, and the path of its line, where notes can be attached to it. */
public class RelationLayout {
    /** The change marks which go below the boxes of the diagram, like the thick line of an added relation. */
    public final List<AbstractSceneElement> underlay;
    public final List<AbstractSceneElement> elements;
    public final Rectangle2D.Double bounds;
    public final ScenePolyline line;
    /** The marks at the ends of the line: diamond, triangle or arrowhead. */
    public final List<AbstractSceneElement> decorations;
    public final List<Point2D.Double> path;
    /** The texts of the relation, which the user can change in place. */
    public final List<SceneTextArea> textAreas;

    public RelationLayout(List<AbstractSceneElement> underlay, List<AbstractSceneElement> elements,
                          Rectangle2D.Double bounds, ScenePolyline line, List<AbstractSceneElement> decorations,
                          List<Point2D.Double> path, List<SceneTextArea> textAreas) {
        this.underlay = List.copyOf(underlay);
        this.elements = List.copyOf(elements);
        this.bounds = bounds;
        this.line = line;
        this.decorations = List.copyOf(decorations);
        this.path = path;
        this.textAreas = List.copyOf(textAreas);
    }
}
