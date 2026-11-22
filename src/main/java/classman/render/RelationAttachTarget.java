package classman.render;

import classman.model.v001.AnchorModel_V001;

import java.awt.geom.Point2D;
import java.util.List;

/** A relation: the line ends at the middle of the longest part of the relation, or at a point of the line of the
 * relation which an anchor fixes, given in percent of its length. */
public class RelationAttachTarget extends AttachTarget {
    private final List<Point2D.Double> path;

    /** @param path the points of the line of the relation, from its start to its end */
    public RelationAttachTarget(List<Point2D.Double> path) {
        this.path = List.copyOf(path);
    }

    @Override
    public Point2D.Double endFacing(Point2D.Double from) {
        return PathGeometry.attachPoint(path);
    }

    @Override
    public Double alignmentX() {
        return PathGeometry.attachPoint(path).x;
    }

    @Override
    public Double alignmentY() {
        return PathGeometry.attachPoint(path).y;
    }

    @Override
    public Point2D.Double pointAlong(double percent) {
        return PathGeometry.pointAlong(path, percent);
    }

    @Override
    public AnchorModel_V001 anchorNear(String id, Point2D point) {
        return AnchorModel_V001.onLine(id, (int) Math.round(PathGeometry.percentNear(path, point)));
    }
}
