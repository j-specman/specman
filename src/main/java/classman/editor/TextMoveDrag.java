package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.TextPlaceModel_V001;
import classman.render.PathGeometry;
import classman.render.PathSpot;
import classman.render.TextKind;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The dragging of the label or a multiplicity of a relation. The text follows the mouse by the distance between where
 * it was grabbed and the middle of the text, so it doesn't jump. Its place is worked out from the point of the line
 * which is closest to the middle of the text, and the distance from the line: a label snaps to the middle of the line
 * if it comes close, so that a label can be put back there easily. Cancelling takes the place back.
 */
class TextMoveDrag extends Drag {
  private static final int MIDDLE_PERCENT = 50;

  private final AbstractRelationModel_V001 relation;
  private final TextKind kind;
  private final List<Point2D> path;
  private final Point2D pressedAt;
  private final Point2D textMiddle;
  private final TextPlaceModel_V001 placeBefore;
  private boolean moved;

  /** @param path the points of the line of the relation
   * @param textArea where the text is now */
  TextMoveDrag(AbstractRelationModel_V001 relation, TextKind kind, List<Point2D> path, Point2D pressedAt,
               Rectangle2D textArea) {
    this.relation = relation;
    this.kind = kind;
    this.path = path;
    this.pressedAt = pressedAt;
    this.textMiddle = new Point2D.Double(textArea.getCenterX(), textArea.getCenterY());
    this.placeBefore = TextPlaces.of(relation, kind);
  }

  @Override
  boolean hasMoved() { return moved; }

  /** @param snapTolerance how close to the middle of the line a label has to come to snap to it */
  @Override
  void to(Point2D at, double snapTolerance) {
    moved = true;
    Point2D middle = new Point2D.Double(textMiddle.getX() + at.getX() - pressedAt.getX(),
      textMiddle.getY() + at.getY() - pressedAt.getY());
    PathSpot spot = PathGeometry.spotNear(path, middle);
    double length = PathGeometry.length(path);
    int across = (int) Math.round(spot.across);
    TextPlaceModel_V001 place = switch (kind) {
      case RELATION_LABEL -> new TextPlaceModel_V001(percentAlong(spot.along, length, snapTolerance), across);
      case FROM_MULTIPLICITY -> new TextPlaceModel_V001((int) Math.round(spot.along), across);
      default -> new TextPlaceModel_V001((int) Math.round(length - spot.along), across);
    };
    TextPlaces.set(relation, kind, place);
  }

  private static int percentAlong(double along, double length, double snapTolerance) {
    if (length == 0 || Math.abs(along - length / 2) <= snapTolerance) {
      return MIDDLE_PERCENT;
    }
    return (int) Math.max(0, Math.min(100, Math.round(along / length * 100)));
  }

  @Override
  void cancel() {
    if (moved) {
      TextPlaces.set(relation, kind, placeBefore);
      moved = false;
    }
  }
}
