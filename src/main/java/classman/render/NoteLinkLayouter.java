package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out the dashed line from a note to the element it is attached to. Without waypoints the line is straight.
 * With waypoints it starts at the note border facing the first one and ends at the target facing the last one.
 */
public class NoteLinkLayouter {

    public ScenePolyline layout(Rectangle2D.Double noteArea, AttachTarget target, List<Point2D.Double> waypoints) {
        List<Point2D.Double> points = new ArrayList<>();
        if (waypoints.isEmpty()) {
            Point2D.Double toTarget = target.endFacing(BoxGeometry.center(noteArea));
            points.add(BoxGeometry.exitPoint(noteArea, toTarget));
            points.add(toTarget);
        } else {
            points.add(BoxGeometry.exitPoint(noteArea, waypoints.get(0)));
            points.addAll(waypoints);
            points.add(target.endFacing(waypoints.get(waypoints.size() - 1)));
        }
        return polyline(points);
    }

    private ScenePolyline polyline(List<Point2D.Double> points) {
        double[] xs = new double[points.size()];
        double[] ys = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            xs[i] = points.get(i).x;
            ys[i] = points.get(i).y;
        }
        return new ScenePolyline(xs, ys, true);
    }
}
