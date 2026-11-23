package classman.render;

import classman.model.v001.AbstractRelationModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Lays out one relation: the line along its path, the marks at its ends, its texts and its change marks. */
public class RelationLayouter {
    private final TextMeasurer measurer;
    private final RelationPathBuilder pathBuilder = new RelationPathBuilder();
    private final RelationDecorator decorator = new RelationDecorator();
    private final RelationTextLayouter textLayouter;
    private final RelationTextAreas textAreaLayouter;
    private final RelationMarkLayouter markLayouter;

    /** @param measurer measures texts in the font */
    public RelationLayouter(TextMeasurer measurer, ChangePalette palette, TextFont font) {
        this.measurer = measurer;
        this.textLayouter = new RelationTextLayouter(measurer, font);
        this.textAreaLayouter = new RelationTextAreas(measurer);
        this.markLayouter = new RelationMarkLayouter(measurer, palette);
    }

    /** @param targets the classes and attributes of the diagram which the relation can join
     * @param boxAreas the areas of all boxes of the diagram, which the texts of the relation keep clear of */
    public RelationLayout layout(AbstractRelationModel_V001 relation, AttachTargetIndex targets,
                                 Collection<Rectangle2D.Double> boxAreas) {
        List<Point2D.Double> path = pathBuilder.pathOf(relation, targets);
        RelationTexts texts = textLayouter.texts(relation, path, boxAreas);
        String lineColor = markLayouter.lineColor(relation);
        List<AbstractSceneElement> underlay = markLayouter.underlay(relation, path, texts);
        List<AbstractSceneElement> elements = new ArrayList<>();
        ScenePolyline line = line(path, relation.type.isDashed(), lineColor);
        elements.add(line);
        List<AbstractSceneElement> decorations = decorator.decorations(relation, path, lineColor,
            markLayouter.triangleFill(relation));
        elements.addAll(decorations);
        elements.addAll(texts.all());
        elements.addAll(markLayouter.overlay(relation, texts, PathGeometry.quarterPoint(path)));
        Rectangle2D.Double bounds = boundsOf(path, underlay, elements);
        List<SceneTextArea> textAreas = new ArrayList<>(textAreaLayouter.of(relation, texts));
        textAreas.addAll(textAreaLayouter.placeholdersOf(relation,
            textLayouter.placeholders(relation, path, boxAreas)));
        return new RelationLayout(underlay, elements, bounds, line, decorations, path, textAreas);
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
