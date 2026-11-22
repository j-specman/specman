package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.RelationType_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lays out one relation: the line along its path, the marks at its ends, its texts and its change marks. */
public class RelationLayouter {
    private final TextMeasurer measurer;
    private final RelationPathBuilder pathBuilder = new RelationPathBuilder();
    private final RelationDecorator decorator = new RelationDecorator();
    private final RelationTextLayouter textLayouter;
    private final RelationTextAreas textAreaLayouter;
    private final RelationMarkLayouter markLayouter;
    private final AssocClassLayouter assocClassLayouter;

    public RelationLayouter(TextMeasurer measurer, ClassBoxLayouter classLayouter, ChangePalette palette) {
        this.measurer = measurer;
        this.textLayouter = new RelationTextLayouter(measurer);
        this.textAreaLayouter = new RelationTextAreas(measurer);
        this.markLayouter = new RelationMarkLayouter(measurer, palette);
        this.assocClassLayouter = new AssocClassLayouter(classLayouter);
    }

    public RelationLayout layout(AbstractRelationModel_V001 relation, Map<String, Rectangle2D.Double> classAreas) {
        List<Point2D.Double> path = pathBuilder.pathOf(relation, classAreas);
        RelationTexts texts = textLayouter.texts(relation, path);
        String lineColor = markLayouter.lineColor(relation);
        List<AbstractSceneElement> underlay = markLayouter.underlay(relation, path, texts);
        List<AbstractSceneElement> elements = new ArrayList<>();
        ScenePolyline line = line(path, relation.type == RelationType_V001.IMPLEMENTS, lineColor);
        elements.add(line);
        List<AbstractSceneElement> decorations = decorator.decorations(relation, path, lineColor);
        elements.addAll(decorations);
        elements.addAll(texts.all());
        elements.addAll(markLayouter.overlay(relation, texts, PathGeometry.quarterPoint(path)));
        Rectangle2D.Double bounds = boundsOf(path, underlay, elements);
        List<SceneElementArea> elementAreas = new ArrayList<>();
        List<SceneTextArea> textAreas = new ArrayList<>(textAreaLayouter.of(relation, texts));
        textAreas.addAll(textAreaLayouter.placeholdersOf(relation, textLayouter.placeholders(relation, path)));
        if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
            RelationLayout assocClass = assocClassLayouter.layout(association.assocClass, path);
            elements.addAll(assocClass.elements);
            bounds = BoxGeometry.union(bounds, assocClass.bounds);
            elementAreas.addAll(assocClass.elementAreas);
            textAreas.addAll(assocClass.textAreas);
        }
        return new RelationLayout(underlay, elements, bounds, line, decorations, path, elementAreas, textAreas);
    }

    private ScenePolyline line(List<Point2D.Double> path, boolean dashed, String color) {
        double[] xs = new double[path.size()];
        double[] ys = new double[path.size()];
        for (int i = 0; i < path.size(); i++) {
            xs[i] = path.get(i).x;
            ys[i] = path.get(i).y;
        }
        return new ScenePolyline(xs, ys, dashed, color, DiagramStyle.STROKE_WIDTH);
    }

    /** The area of the path, the marks at its ends, the texts and the change marks. */
    private Rectangle2D.Double boundsOf(List<Point2D.Double> path, List<AbstractSceneElement> underlay,
                                       List<AbstractSceneElement> elements) {
        Rectangle2D.Double bounds = null;
        for (Point2D.Double point : path) {
            bounds = include(bounds, point.x, point.y, 0);
        }
        List<AbstractSceneElement> parts = new ArrayList<>(underlay);
        parts.addAll(elements);
        for (AbstractSceneElement element : parts) {
            if (element instanceof ScenePolygon polygon) {
                for (int i = 0; i < polygon.xs.length; i++) {
                    bounds = include(bounds, polygon.xs[i], polygon.ys[i], 0);
                }
            } else if (element instanceof ScenePolyline polyline) {
                for (int i = 0; i < polyline.xs.length; i++) {
                    bounds = include(bounds, polyline.xs[i], polyline.ys[i], polyline.strokeWidth / 2);
                }
            } else if (element instanceof SceneText text) {
                bounds = include(bounds, text.left(), text.y - measurer.ascent(), 0);
                bounds = include(bounds, text.left() + text.width, text.y - measurer.ascent() + measurer.lineHeight(), 0);
            } else if (element instanceof SceneRectangle rectangle) {
                bounds = include(bounds, rectangle.x, rectangle.y, 0);
                bounds = include(bounds, rectangle.x + rectangle.width, rectangle.y + rectangle.height, 0);
            }
        }
        return bounds;
    }

    private Rectangle2D.Double include(Rectangle2D.Double bounds, double x, double y, double margin) {
        if (bounds == null) {
            return new Rectangle2D.Double(x - margin, y - margin, 2 * margin, 2 * margin);
        }
        bounds.add(x - margin, y - margin);
        bounds.add(x + margin, y + margin);
        return bounds;
    }
}
