package classman.render;

import classman.model.v001.AbstractRelationModel_V001;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** The line a relation is drawn along with the marks at its ends, and the relation it belongs to. */
public class SceneRelationLine {
    public final AbstractRelationModel_V001 relation;
    public final ScenePolyline line;
    /** Diamond, triangle or arrowhead, which are drawn above the line. */
    public final List<AbstractSceneElement> decorations;

    public SceneRelationLine(AbstractRelationModel_V001 relation, ScenePolyline line,
                             List<AbstractSceneElement> decorations) {
        this.relation = relation;
        this.line = line;
        this.decorations = List.copyOf(decorations);
    }

    /** @return the same line and marks, all drawn with another stroke width and with round corners, so that the sharp
     * tips of the marks don't poke far into the box they point at */
    public SceneRelationLine withStrokeWidth(double width) {
        List<AbstractSceneElement> widerDecorations = new ArrayList<>();
        for (AbstractSceneElement decoration : decorations) {
            widerDecorations.add(withStrokeWidth(decoration, width));
        }
        return new SceneRelationLine(relation, line.withStrokeWidth(width).withRoundCorners(), widerDecorations);
    }

    private static AbstractSceneElement withStrokeWidth(AbstractSceneElement element, double width) {
        if (element instanceof ScenePolygon polygon) {
            return polygon.withStrokeWidth(width).withRoundCorners();
        }
        if (element instanceof ScenePolyline polyline) {
            return polyline.withStrokeWidth(width).withRoundCorners();
        }
        return element;
    }

    /** @return the points of the line: where it starts, its waypoints, where it ends */
    public List<Point2D> points() {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < line.xs.length; i++) {
            points.add(new Point2D.Double(line.xs[i], line.ys[i]));
        }
        return points;
    }

    /** @return the distance of the point to the closest part of the line */
    public double distanceTo(Point2D point) {
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < line.xs.length - 1; i++) {
            closest = Math.min(closest, Line2D.ptSegDist(line.xs[i], line.ys[i], line.xs[i + 1], line.ys[i + 1],
                point.getX(), point.getY()));
        }
        return closest;
    }
}
