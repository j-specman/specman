package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.LOOP_SIZE;

/** Finds the points a relation runs through. */
public class RelationPathBuilder {

    /**
     * @param classAreas where the classes are, by ID
     * @throws IllegalArgumentException if the relation refers to a class which isn't in the diagram
     */
    public List<Point2D.Double> pathOf(AbstractRelationModel_V001 relation, Map<String, Rectangle2D.Double> classAreas) {
        Rectangle2D.Double from = areaOf(relation.fromId, classAreas);
        Rectangle2D.Double to = areaOf(relation.toId, classAreas);
        if (relation.route != null) {
            return routedPath(relation.route, classAreas);
        }
        if (relation.fromId.equals(relation.toId)) {
            return loopPath(from);
        }
        List<Point2D.Double> path = new ArrayList<>();
        path.add(BoxGeometry.exitPoint(from, BoxGeometry.center(to)));
        path.add(BoxGeometry.exitPoint(to, BoxGeometry.center(from)));
        return path;
    }

    private List<Point2D.Double> routedPath(RouteModel_V001 route, Map<String, Rectangle2D.Double> classAreas) {
        List<Point2D.Double> path = new ArrayList<>();
        path.add(anchorPoint(route.start, classAreas));
        for (PositionModel_V001 waypoint : route.waypoints) {
            path.add(new Point2D.Double(waypoint.x, waypoint.y));
        }
        path.add(anchorPoint(route.end, classAreas));
        return path;
    }

    private Point2D.Double anchorPoint(AnchorModel_V001 anchor, Map<String, Rectangle2D.Double> classAreas) {
        return BoxGeometry.sidePoint(areaOf(anchor.classId, classAreas), anchor.side, anchor.offset);
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

    private Rectangle2D.Double areaOf(String classId, Map<String, Rectangle2D.Double> classAreas) {
        Rectangle2D.Double area = classAreas.get(classId);
        if (area == null) {
            throw new IllegalArgumentException("The diagram has no class with the ID " + classId);
        }
        return area;
    }
}
