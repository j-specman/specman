package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.RelationType_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.ARROW_HALF_WIDTH;
import static classman.render.DiagramStyle.ARROW_LENGTH;
import static classman.render.DiagramStyle.DIAMOND_HALF_WIDTH;
import static classman.render.DiagramStyle.DIAMOND_LENGTH;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.TRIANGLE_HALF_WIDTH;
import static classman.render.DiagramStyle.TRIANGLE_LENGTH;
import static classman.render.DiagramStyle.WHITE;

/** Draws the marks at the ends of a relation: diamond, arrowhead or triangle. */
public class RelationDecorator {

    public List<AbstractSceneElement> decorations(AbstractRelationModel_V001 relation, List<Point2D.Double> path) {
        List<AbstractSceneElement> decorations = new ArrayList<>();
        Point2D.Double start = path.get(0);
        Point2D.Double end = path.get(path.size() - 1);
        double[] startDirection = directionAway(path, 0, 1);
        double[] endDirection = directionAway(path, path.size() - 1, -1);
        if (relation.type == RelationType_V001.AGGREGATION) {
            decorations.add(diamond(start, startDirection, WHITE));
        } else if (relation.type == RelationType_V001.COMPOSITION) {
            decorations.add(diamond(start, startDirection, STROKE));
        }
        if (relation.type == RelationType_V001.EXTENDS || relation.type == RelationType_V001.IMPLEMENTS) {
            decorations.add(triangle(end, endDirection));
        } else if (isDirected(relation)) {
            decorations.add(arrow(end, endDirection));
        }
        return decorations;
    }

    private boolean isDirected(AbstractRelationModel_V001 relation) {
        return relation instanceof AssociationRelationModel_V001 association && association.directed;
    }

    /**
     * The unit vector from the point at the given index into the line, away from the box the line starts or ends at.
     * It looks at the next point which is not the same, so a doubled point doesn't spoil the direction.
     */
    private double[] directionAway(List<Point2D.Double> path, int index, int step) {
        Point2D.Double tip = path.get(index);
        for (int i = index + step; i >= 0 && i < path.size(); i += step) {
            double dx = path.get(i).x - tip.x;
            double dy = path.get(i).y - tip.y;
            double length = Math.hypot(dx, dy);
            if (length > 0) {
                return new double[]{dx / length, dy / length};
            }
        }
        return new double[]{0, 1};
    }

    private ScenePolygon diamond(Point2D.Double tip, double[] direction, String fill) {
        double[] middle = along(tip, direction, DIAMOND_LENGTH / 2);
        double[] far = along(tip, direction, DIAMOND_LENGTH);
        double sideX = -direction[1] * DIAMOND_HALF_WIDTH;
        double sideY = direction[0] * DIAMOND_HALF_WIDTH;
        return new ScenePolygon(
            new double[]{tip.x, middle[0] + sideX, far[0], middle[0] - sideX},
            new double[]{tip.y, middle[1] + sideY, far[1], middle[1] - sideY}, fill, STROKE);
    }

    private ScenePolygon triangle(Point2D.Double tip, double[] direction) {
        double[] base = along(tip, direction, TRIANGLE_LENGTH);
        double sideX = -direction[1] * TRIANGLE_HALF_WIDTH;
        double sideY = direction[0] * TRIANGLE_HALF_WIDTH;
        return new ScenePolygon(
            new double[]{tip.x, base[0] + sideX, base[0] - sideX},
            new double[]{tip.y, base[1] + sideY, base[1] - sideY}, WHITE, STROKE);
    }

    private ScenePolyline arrow(Point2D.Double tip, double[] direction) {
        double[] base = along(tip, direction, ARROW_LENGTH);
        double sideX = -direction[1] * ARROW_HALF_WIDTH;
        double sideY = direction[0] * ARROW_HALF_WIDTH;
        return new ScenePolyline(
            new double[]{base[0] - sideX, tip.x, base[0] + sideX},
            new double[]{base[1] - sideY, tip.y, base[1] + sideY}, false);
    }

    private double[] along(Point2D.Double start, double[] direction, double distance) {
        return new double[]{start.x + direction[0] * distance, start.y + direction[1] * distance};
    }
}
