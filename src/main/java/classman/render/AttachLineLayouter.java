package classman.render;

import classman.model.v001.AnchorModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out the dashed line from a note or a class to the element it is attached to, the line of an anchor line or an association class link. Without
 * waypoints the line is straight.
 * With waypoints it starts at the border of its box facing the first one and ends at the target facing the last one. An
 * end which has an anchor starts or ends at that point of the border instead, and the other end faces it.
 */
public class AttachLineLayouter {

    /** @param startAnchor where the line starts on its box, null if that is computed
     * @param endAnchor where the line ends on the target, null if that is computed or the anchor doesn't fit the target
     * @param color the color of the dashed line */
    public ScenePolyline layout(Rectangle2D.Double boxArea, AttachTarget target, List<Point2D.Double> waypoints,
                                AnchorModel_V001 startAnchor, AnchorModel_V001 endAnchor, String color) {
        Point2D.Double fixedStart = startAnchor == null ? null
            : BoxGeometry.sidePoint(boxArea, startAnchor.side, startAnchor.offset);
        Point2D.Double fixedEnd = endAnchor == null ? null : target.fixedPoint(endAnchor);
        Point2D.Double end = fixedEnd != null ? fixedEnd : target.endFacing(endLooksFrom(boxArea, waypoints, fixedStart));
        Point2D.Double start = fixedStart != null ? fixedStart
            : BoxGeometry.exitPoint(boxArea, waypoints.isEmpty() ? end : waypoints.get(0));
        List<Point2D.Double> points = new ArrayList<>();
        points.add(start);
        points.addAll(waypoints);
        points.add(end);
        return polyline(points, color);
    }

    /** The point the end of the line faces: the last waypoint, else the start if it is fixed, else the box. */
    private Point2D.Double endLooksFrom(Rectangle2D.Double boxArea, List<Point2D.Double> waypoints,
                                        Point2D.Double fixedStart) {
        if (!waypoints.isEmpty()) {
            return waypoints.get(waypoints.size() - 1);
        }
        return fixedStart != null ? fixedStart : BoxGeometry.center(boxArea);
    }

    private ScenePolyline polyline(List<Point2D.Double> points, String color) {
        double[] xs = new double[points.size()];
        double[] ys = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            xs[i] = points.get(i).x;
            ys[i] = points.get(i).y;
        }
        return new ScenePolyline(xs, ys, true, color, DiagramStyle.STROKE_WIDTH);
    }
}
