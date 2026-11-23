package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.Scene;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The dragging of several boxes together over the diagram. Each box and each waypoint of the lines between them moves
 * by the distance which the mouse has moved, but everything stops as a whole at the left and upper edge of the diagram,
 * so the boxes keep their distances. The other boxes keep their places, see {@link PositionFreezer}. */
class GroupDrag extends Drag {
  private final AbstractBoxModel_V001 pressedBox;
  private final Point2D pressedAt;
  private final Map<AbstractBoxModel_V001, PositionModel_V001> startPositions = new LinkedHashMap<>();
  /** Keyed by the identity of the lists, as their content changes while they are moved. */
  private final Map<List<PositionModel_V001>, List<PositionModel_V001>> startWaypoints = new IdentityHashMap<>();
  private final int mostLeft;
  private final int mostUp;
  private final PositionFreezer freezer;

  /** @param pressedBox the member of the group which the mouse was pressed on
   * @param startScene the diagram before the drag
   * @param waypointLists the lists of waypoints, as they are in the model, of the lines which move with the group */
  GroupDrag(List<AbstractBoxModel_V001> members, AbstractBoxModel_V001 pressedBox, Point2D pressedAt, Scene startScene,
            PositionFreezer freezer, List<List<PositionModel_V001>> waypointLists) {
    this.pressedBox = pressedBox;
    this.pressedAt = pressedAt;
    this.freezer = freezer;
    int left = Integer.MAX_VALUE;
    int up = Integer.MAX_VALUE;
    for (AbstractBoxModel_V001 member : members) {
      PositionModel_V001 position = new PositionModel_V001(
        (int) Math.round(startScene.elementAreaOf(member).area.getX()),
        (int) Math.round(startScene.elementAreaOf(member).area.getY()));
      startPositions.put(member, position);
      left = Math.min(left, position.x);
      up = Math.min(up, position.y);
    }
    for (List<PositionModel_V001> waypoints : waypointLists) {
      startWaypoints.put(waypoints, new ArrayList<>(waypoints));
      for (PositionModel_V001 waypoint : waypoints) {
        left = Math.min(left, waypoint.x);
        up = Math.min(up, waypoint.y);
      }
    }
    this.mostLeft = left;
    this.mostUp = up;
  }

  /** @return the member of the group which the mouse was pressed on */
  AbstractBoxModel_V001 pressedBox() { return pressedBox; }

  @Override
  boolean hasMoved() { return freezer.isFrozen(); }

  /** Moves all boxes and waypoints by the distance between the point where the mouse was pressed and the given one, rounded to
   * whole units and limited so that no box and no waypoint gets left of or above the top left corner of the diagram. */
  @Override
  void to(Point2D at, double snapTolerance) {
    freezer.freeze();
    int dx = Math.max(-mostLeft, (int) Math.round(at.getX() - pressedAt.getX()));
    int dy = Math.max(-mostUp, (int) Math.round(at.getY() - pressedAt.getY()));
    startPositions.forEach((box, start) -> box.position = new PositionModel_V001(start.x + dx, start.y + dy));
    startWaypoints.forEach((waypoints, start) -> {
      for (int i = 0; i < start.size(); i++) {
        waypoints.set(i, new PositionModel_V001(start.get(i).x + dx, start.get(i).y + dy));
      }
    });
  }

  /** Puts everything back where it was before the first move. */
  @Override
  void cancel() {
    freezer.restore();
    startWaypoints.forEach((waypoints, start) -> {
      for (int i = 0; i < start.size(); i++) {
        waypoints.set(i, start.get(i));
      }
    });
  }
}
