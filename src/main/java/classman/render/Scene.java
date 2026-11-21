package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Point2D;
import java.util.List;

/** Everything which is drawn for a diagram, and the area it covers. */
public class Scene {
    public final List<AbstractSceneElement> elements;
    /** Where the boxes of the diagram are, in the order they are drawn. */
    public final List<SceneElementArea> elementAreas;
    /** Where the texts are which the user can change in place, in the order they are drawn. */
    public final List<SceneTextArea> textAreas;
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Scene(List<AbstractSceneElement> elements, double x, double y, double width, double height) {
        this(elements, List.of(), List.of(), x, y, width, height);
    }

    public Scene(List<AbstractSceneElement> elements, List<SceneElementArea> elementAreas,
                 List<SceneTextArea> textAreas, double x, double y, double width, double height) {
        this.elements = List.copyOf(elements);
        this.elementAreas = List.copyOf(elementAreas);
        this.textAreas = List.copyOf(textAreas);
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

    /** @return the text at the point, the one drawn on top if they overlap, null if there is none */
    public SceneTextArea textAreaAt(Point2D point) {
        for (int i = textAreas.size() - 1; i >= 0; i--) {
            if (textAreas.get(i).area.contains(point)) {
                return textAreas.get(i);
            }
        }
        return null;
    }
}
