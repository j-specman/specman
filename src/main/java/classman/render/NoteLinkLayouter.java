package classman.render;

import classman.model.v001.AnchorModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out the dashed line from a note to the element it is attached to, the line of an anchor line. Without
 * waypoints the line is straight.
 * With waypoints it starts at the note border facing the first one and ends at the target facing the last one. An
 * end which has an anchor starts or ends at that point of the border instead, and the other end faces it.
 */
public class NoteLinkLayouter {

    /** @param startAnchor where the line starts on the note, null if that is computed
     * @param endAnchor where the line ends on the target, null if that is computed or the anchor doesn't fit the target
     * @param color the color of the dashed line */
    public ScenePolyline layout(Rectangle2D.Double noteArea, AttachTarget target, List<Point2D.Double> waypoints,
                                AnchorModel_V001 startAnchor, AnchorModel_V001 endAnchor, String color) {
        Point2D.Double fixedStart = startAnchor == null ? null
            : BoxGeometry.sidePoint(noteArea, startAnchor.side, startAnchor.offset);
        Point2D.Double fixedEnd = endAnchor == null ? null : target.fixedPoint(endAnchor);
        Point2D.Double end = fixedEnd != null ? fixedEnd : target.endFacing(endLooksFrom(noteArea, waypoints, fixedStart));
        Point2D.Double start = fixedStart != null ? fixedStart
            : BoxGeometry.exitPoint(noteArea, waypoints.isEmpty() ? end : waypoints.get(0));
        List<Point2D.Double> points = new ArrayList<>();
        points.add(start);
        points.addAll(waypoints);
        points.add(end);
        return polyline(points, color);
    }

    /** The point the end of the line faces: the last waypoint, else the start if it is fixed, else the note. */
    private Point2D.Double endLooksFrom(Rectangle2D.Double noteArea, List<Point2D.Double> waypoints,
                                        Point2D.Double fixedStart) {
        if (!waypoints.isEmpty()) {
            return waypoints.get(waypoints.size() - 1);
        }
        return fixedStart != null ? fixedStart : BoxGeometry.center(noteArea);
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
