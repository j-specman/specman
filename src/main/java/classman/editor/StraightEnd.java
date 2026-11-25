package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.Side_V001;
import classman.render.BoxGeometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * An end of a route which sits on a side of its box and is joined to the dragged point by a horizontal or vertical
 * part of the line. While the point is dragged, the end slides along its side with it, as far as the side reaches, so
 * that the part stays horizontal or vertical. It does so only for a part which is straight when the drag starts, and
 * only at a side the end can slide along without leaving the line's direction: the left and right side for a
 * horizontal part, the top and bottom side for a vertical one.
 */
class StraightEnd {
  /** How far a part of the line may be off the axis, in pixels, and still count as horizontal or vertical. */
  private static final double AXIS_TOLERANCE = 0.5;

  private final boolean atStart;
  private final String classId;
  private final Side_V001 side;
  private final Rectangle2D area;

  private StraightEnd(boolean atStart, String classId, Side_V001 side, Rectangle2D area) {
    this.atStart = atStart;
    this.classId = classId;
    this.side = side;
    this.area = area;
  }

  /**
   * @param path the points of the line before the drag, the handles are these points
   * @param handle the index of the dragged point in the path
   * @param route the route of the relation, the ends of which are the anchors of the first and last point
   * @param fromArea where the class of the start is, toArea where the class of the end is
   * @return the ends which follow the dragged point, none if it is not joined directly to an end
   */
  static List<StraightEnd> following(List<Point2D> path, int handle, RouteModel_V001 route, Rectangle2D fromArea,
                                     Rectangle2D toArea) {
    List<StraightEnd> ends = new ArrayList<>();
    addIfStraight(ends, path, handle, 0, route.start, fromArea, true);
    addIfStraight(ends, path, handle, path.size() - 1, route.end, toArea, false);
    return ends;
  }

  private static void addIfStraight(List<StraightEnd> ends, List<Point2D> path, int handle, int endIndex,
                                    AnchorModel_V001 anchor, Rectangle2D area, boolean atStart) {
    boolean joined = Math.abs(endIndex - handle) == 1;
    if (!joined || anchor.side == null) {
      return;
    }
    double dx = path.get(handle).getX() - path.get(endIndex).getX();
    double dy = path.get(handle).getY() - path.get(endIndex).getY();
    boolean horizontal = Math.abs(dy) < AXIS_TOLERANCE && Math.abs(dx) >= AXIS_TOLERANCE;
    boolean vertical = Math.abs(dx) < AXIS_TOLERANCE && Math.abs(dy) >= AXIS_TOLERANCE;
    boolean slidesAlongLine = horizontal && (anchor.side == Side_V001.LEFT || anchor.side == Side_V001.RIGHT)
      || vertical && (anchor.side == Side_V001.TOP || anchor.side == Side_V001.BOTTOM);
    if (slidesAlongLine) {
      ends.add(new StraightEnd(atStart, anchor.classId, anchor.side, area));
    }
  }

  /** Puts the end at the place on its side which is level with the dragged point. */
  void follow(RouteEditing routeEditing, AbstractRelationModel_V001 relation, Point2D dragged) {
    AnchorModel_V001 anchor = new AnchorModel_V001(classId, side, BoxGeometry.offsetOn(side, area, dragged));
    if (atStart) {
      routeEditing.slideStart(relation, anchor);
    }
    else {
      routeEditing.slideEnd(relation, anchor);
    }
  }
}
