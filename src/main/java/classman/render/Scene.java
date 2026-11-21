package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Everything which is drawn for a diagram, and the area it covers. */
public class Scene {
    public final List<AbstractSceneElement> elements;
    /** Where the boxes of the diagram are, in the order they are drawn. */
    public final List<SceneElementArea> elementAreas;
    /** Where the texts are which the user can change in place, in the order they are drawn. */
    public final List<SceneTextArea> textAreas;
    /** Where the texts would stand which a relation doesn't have yet, so that the user can create them. They are no
     * part of the picture. */
    public final List<SceneTextArea> placeholderAreas;
    /** The lines of the relations, in the order they are drawn. */
    public final List<SceneRelationLine> relationLines;
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Scene(List<AbstractSceneElement> elements, double x, double y, double width, double height) {
        this(elements, List.of(), List.of(), List.of(), x, y, width, height);
    }

    /** @param textAreas the areas of the texts, including the placeholders, which the scene keeps apart */
    public Scene(List<AbstractSceneElement> elements, List<SceneElementArea> elementAreas,
                 List<SceneTextArea> textAreas, List<SceneRelationLine> relationLines,
                 double x, double y, double width, double height) {
        this.elements = List.copyOf(elements);
        this.elementAreas = List.copyOf(elementAreas);
        this.textAreas = textAreas.stream().filter(area -> !area.placeholder).toList();
        this.placeholderAreas = textAreas.stream().filter(area -> area.placeholder).toList();
        this.relationLines = List.copyOf(relationLines);
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

    /** @return the line of the relation which is at most the tolerance away from the point, the closest one if there
     * are several, null if there is none */
    public SceneRelationLine relationLineAt(Point2D point, double tolerance) {
        SceneRelationLine closest = null;
        double closestDistance = tolerance;
        for (SceneRelationLine relationLine : relationLines) {
            double distance = relationLine.distanceTo(point);
            if (distance <= closestDistance) {
                closest = relationLine;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /** @return where the relation's line is drawn, null if it is not a relation of this scene */
    public SceneRelationLine relationLineOf(AbstractDiagramElementModel_V001 relation) {
        for (SceneRelationLine relationLine : relationLines) {
            if (relationLine.relation == relation) {
                return relationLine;
            }
        }
        return null;
    }

    /** @return the same scene with the line of the relation and the marks at its ends drawn with another stroke width,
     * each in its place in the order of drawing, so that the marks stay above the line */
    public Scene withStrokeWidth(SceneRelationLine relationLine, double strokeWidth) {
        SceneRelationLine wider = relationLine.withStrokeWidth(strokeWidth);
        Map<AbstractSceneElement, AbstractSceneElement> replacements = new IdentityHashMap<>();
        replacements.put(relationLine.line, wider.line);
        for (int i = 0; i < wider.decorations.size(); i++) {
            replacements.put(relationLine.decorations.get(i), wider.decorations.get(i));
        }
        List<AbstractSceneElement> widerElements = new ArrayList<>();
        for (AbstractSceneElement element : elements) {
            widerElements.add(replacements.getOrDefault(element, element));
        }
        List<SceneRelationLine> widerLines = new ArrayList<>();
        for (SceneRelationLine candidate : relationLines) {
            widerLines.add(candidate == relationLine ? wider : candidate);
        }
        List<SceneTextArea> allTextAreas = new ArrayList<>(textAreas);
        allTextAreas.addAll(placeholderAreas);
        return new Scene(widerElements, elementAreas, allTextAreas, widerLines, x, y, width, height);
    }

    /** @return the placeholder at the point, null if there is none */
    public SceneTextArea placeholderAt(Point2D point) {
        for (int i = placeholderAreas.size() - 1; i >= 0; i--) {
            if (placeholderAreas.get(i).hitArea().contains(point)) {
                return placeholderAreas.get(i);
            }
        }
        return null;
    }

    /** @return the text at the point, the one drawn on top if they overlap, null if there is none */
    public SceneTextArea textAreaAt(Point2D point) {
        for (int i = textAreas.size() - 1; i >= 0; i--) {
            if (textAreas.get(i).hitArea().contains(point)) {
                return textAreas.get(i);
            }
        }
        return null;
    }
}
