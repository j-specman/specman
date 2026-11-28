package classman.editor;

import classman.model.v001.AnchorModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Keeps the end of a line next to a dragged waypoint from running through its box: where the line from the waypoint to
 * an end which is fixed on a side of its box would cross the box, the end is fixed where the line enters the box instead,
 * like {@link LineEndRepair} does it while boxes are moved. Handles which are no waypoint, and ends which are not next
 * to the waypoint, are left alone. */
class WaypointEndRepair {
  private final boolean isNextToStart;
  private final boolean isNextToEnd;
  private final Rectangle2D startBox;
  private final Rectangle2D endBox;

  /** @param handle the index of the dragged point in the path of the line: 0 is the start, the last the end
   * @param pathSize the number of points of the path
   * @param startBox where the box is which the start is fixed on, null if it isn't fixed on a box
   * @param endBox where the box is which the end is fixed on, null if it isn't fixed on a box */
  WaypointEndRepair(int handle, int pathSize, Rectangle2D startBox, Rectangle2D endBox) {
    boolean isWaypoint = handle > 0 && handle < pathSize - 1;
    this.isNextToStart = isWaypoint && handle == 1;
    this.isNextToEnd = isWaypoint && handle == pathSize - 2;
    this.startBox = startBox;
    this.endBox = endBox;
  }

  /** @return true if the dragged handle is a waypoint next to an end of the line */
  boolean isRepairing() {
    return isNextToStart || isNextToEnd;
  }

  /** @return the anchor of the start the way the line needs it with the waypoint where it is dragged to */
  AnchorModel_V001 start(AnchorModel_V001 anchor, Point2D waypoint) {
    return isNextToStart && startBox != null ? LineEndRepair.repaired(anchor, startBox, waypoint) : anchor;
  }

  /** @return the anchor of the end the way the line needs it with the waypoint where it is dragged to */
  AnchorModel_V001 end(AnchorModel_V001 anchor, Point2D waypoint) {
    return isNextToEnd && endBox != null ? LineEndRepair.repaired(anchor, endBox, waypoint) : anchor;
  }
}
