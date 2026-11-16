package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.RelationType_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lays out one relation: the line along its path, the marks at its ends, and its texts. */
public class RelationLayouter {
    private final TextMeasurer measurer;
    private final RelationPathBuilder pathBuilder = new RelationPathBuilder();
    private final RelationDecorator decorator = new RelationDecorator();
    private final RelationTextLayouter textLayouter;
    private final AssocClassLayouter assocClassLayouter;

    public RelationLayouter(TextMeasurer measurer, ClassBoxLayouter classLayouter) {
        this.measurer = measurer;
        this.textLayouter = new RelationTextLayouter(measurer);
        this.assocClassLayouter = new AssocClassLayouter(classLayouter);
    }

    public RelationLayout layout(AbstractRelationModel_V001 relation, Map<String, Rectangle2D.Double> classAreas) {
        List<Point2D.Double> path = pathBuilder.pathOf(relation, classAreas);
        List<AbstractSceneElement> elements = new ArrayList<>();
        elements.add(line(path, relation.type == RelationType_V001.IMPLEMENTS));
        elements.addAll(decorator.decorations(relation, path));
        elements.addAll(textLayouter.texts(relation, path));
        Rectangle2D.Double bounds = boundsOf(path, elements);
        if (relation instanceof AssociationRelationModel_V001 association && association.assocClass != null) {
            RelationLayout assocClass = assocClassLayouter.layout(association.assocClass, path);
            elements.addAll(assocClass.elements);
            bounds = BoxGeometry.union(bounds, assocClass.bounds);
        }
        return new RelationLayout(elements, bounds, PathGeometry.attachPoint(path));
    }

    private ScenePolyline line(List<Point2D.Double> path, boolean dashed) {
        double[] xs = new double[path.size()];
        double[] ys = new double[path.size()];
        for (int i = 0; i < path.size(); i++) {
            xs[i] = path.get(i).x;
            ys[i] = path.get(i).y;
        }
        return new ScenePolyline(xs, ys, dashed);
    }

    /** The area of the path, the marks at its ends and the texts. */
    private Rectangle2D.Double boundsOf(List<Point2D.Double> path, List<AbstractSceneElement> elements) {
        Rectangle2D.Double bounds = null;
        for (Point2D.Double point : path) {
            bounds = include(bounds, point.x, point.y);
        }
        for (AbstractSceneElement element : elements) {
            if (element instanceof ScenePolygon polygon) {
                for (int i = 0; i < polygon.xs.length; i++) {
                    bounds = include(bounds, polygon.xs[i], polygon.ys[i]);
                }
            } else if (element instanceof ScenePolyline polyline) {
                for (int i = 0; i < polyline.xs.length; i++) {
                    bounds = include(bounds, polyline.xs[i], polyline.ys[i]);
                }
            } else if (element instanceof SceneText text) {
                bounds = include(bounds, text.left(), text.y - measurer.ascent());
                bounds = include(bounds, text.left() + text.width, text.y - measurer.ascent() + measurer.lineHeight());
            }
        }
        return bounds;
    }

    private Rectangle2D.Double include(Rectangle2D.Double bounds, double x, double y) {
        if (bounds == null) {
            return new Rectangle2D.Double(x, y, 0, 0);
        }
        bounds.add(x, y);
        return bounds;
    }
}
