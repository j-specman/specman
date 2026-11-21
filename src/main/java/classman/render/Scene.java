package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Point2D;
import java.util.List;

/** Everything which is drawn for a diagram, and the area it covers. */
public class Scene {
    public final List<AbstractSceneElement> elements;
    /** Where the boxes of the diagram are, in the order they are drawn. */
    public final List<SceneElementArea> elementAreas;
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Scene(List<AbstractSceneElement> elements, double x, double y, double width, double height) {
        this(elements, List.of(), x, y, width, height);
    }

    public Scene(List<AbstractSceneElement> elements, List<SceneElementArea> elementAreas,
                 double x, double y, double width, double height) {
        this.elements = List.copyOf(elements);
        this.elementAreas = List.copyOf(elementAreas);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** @return the box at the point, the one drawn on top if boxes overlap, null if there is none */
    public SceneElementArea elementAreaAt(Point2D point) {
        for (int i = elementAreas.size() - 1; i >= 0; i--) {
            if (elementAreas.get(i).area.contains(point)) {
                return elementAreas.get(i);
            }
        }
        return null;
    }

    /** @return where the element is drawn, null if it is not a box of this scene */
    public SceneElementArea elementAreaOf(AbstractDiagramElementModel_V001 element) {
        for (SceneElementArea elementArea : elementAreas) {
            if (elementArea.element == element) {
                return elementArea;
            }
        }
        return null;
    }
}
