package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.RouteModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.LOOP_SIZE;

/** Finds the points a relation runs through. */
public class RelationPathBuilder {

    /**
     * @param targets the classes and attributes of the diagram, by ID, which the ends of the relation lead to
     * @throws IllegalArgumentException if the relation refers to a class which isn't in the diagram, or to an
     * attribute, which only a dependency may do
     */
    public List<Point2D.Double> pathOf(AbstractRelationModel_V001 relation, AttachTargetIndex targets) {
        boolean membersToo = relation.type == RelationType_V001.DEPENDENCY;
        AttachTarget from = targets.findEnd(relation.fromId, membersToo);
        AttachTarget to = targets.findEnd(relation.toId, membersToo);
        if (relation.route != null) {
            return routedPath(relation.route, from, to);
        }
        if (relation.fromId.equals(relation.toId)) {
            return loopPath(from.box());
        }
        List<Point2D.Double> path = new ArrayList<>();
        path.add(from.endFacing(to.center()));
        path.add(to.endFacing(from.center()));
        return path;
    }

    /** An end which is linked to the center of its class leaves the class towards the next point of the line: the first
     * waypoint, else the other end, else the center of the other class. The same holds for an end on an attribute,
     * which leaves it at the side facing the next point. */
    private List<Point2D.Double> routedPath(RouteModel_V001 route, AttachTarget fromTarget, AttachTarget toTarget) {
        List<Point2D.Double> path = new ArrayList<>();
        List<Point2D.Double> inner = new ArrayList<>();
        for (PositionModel_V001 waypoint : route.waypoints) {
            inner.add(new Point2D.Double(waypoint.x, waypoint.y));
        }
        Point2D.Double start = route.start.center ? null : fromTarget.fixedPoint(route.start);
        Point2D.Double end = route.end.center ? null : toTarget.fixedPoint(route.end);
        if (start == null) {
            Point2D.Double towards = !inner.isEmpty() ? inner.get(0) : end != null ? end : toTarget.center();
            start = fromTarget.endFacing(towards);
        }
        if (end == null) {
            Point2D.Double towards = !inner.isEmpty() ? inner.get(inner.size() - 1)
                : route.start.center ? fromTarget.center() : start;
            end = toTarget.endFacing(towards);
        }
        path.add(start);
        path.addAll(inner);
        path.add(end);
        return path;
    }

    /** Up from the top side at three quarters of its width, over the corner, down to the right side at a quarter. */
    private List<Point2D.Double> loopPath(Rectangle2D.Double box) {
        double top = box.y - LOOP_SIZE;
        double right = box.getMaxX() + LOOP_SIZE;
        List<Point2D.Double> path = new ArrayList<>();
        path.add(new Point2D.Double(box.x + box.width * 0.75, box.y));
        path.add(new Point2D.Double(box.x + box.width * 0.75, top));
        path.add(new Point2D.Double(right, top));
        path.add(new Point2D.Double(right, box.y + box.height * 0.25));
        path.add(new Point2D.Double(box.getMaxX(), box.y + box.height * 0.25));
        return path;
    }
}
