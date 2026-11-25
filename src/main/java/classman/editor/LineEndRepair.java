package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.render.BoxGeometry;
import classman.render.Scene;
import classman.render.SceneConnectionLine;
import classman.render.SceneElementArea;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Keeps lines from running through the boxes at their ends while boxes are moved. A moved box which comes to lie over a
 * waypoint of a line which ends at it eats the waypoint, so the line doesn't have to run to it. And where a line which is
 * fixed on a side of its box would run through the box, because the box or the one at the other end of the line has been
 * moved, the end is fixed where the line enters the box instead. Everything is worked out from how the lines were when the
 * move started, so moving back brings waypoints and ends back. Lines which move rigidly with the boxes, because all boxes
 * they join are moved, are left alone. Ends which are linked to the center of a box, or fixed on an attribute or a
 * relation, are left alone too. */
class LineEndRepair {
  private final ClassDiagramModel_V001 model;
  private final ElementOwners owners;
  private final Map<AbstractRelationModel_V001, LineSnapshot> startOfRelations = new LinkedHashMap<>();
  private final Map<AttachLineModel_V001, LineSnapshot> startOfAttachLines = new LinkedHashMap<>();

  /** @param movedBoxes the boxes which are being moved */
  LineEndRepair(ClassDiagramModel_V001 model, Collection<AbstractBoxModel_V001> movedBoxes) {
    this.model = model;
    this.owners = new ElementOwners(model);
    for (AbstractRelationModel_V001 relation : model.relations()) {
      List<AbstractBoxModel_V001> involved = involvedBoxes(relation);
      if (relation.route != null && isAffected(involved, movedBoxes)) {
        startOfRelations.put(relation, new LineSnapshot(relation.route, null, null, List.of(), movedOf(involved, movedBoxes)));
      }
    }
    for (AttachLineModel_V001 attachLine : model.attachLines()) {
      List<AbstractBoxModel_V001> involved = involvedBoxes(attachLine);
      if (isAffected(involved, movedBoxes)) {
        startOfAttachLines.put(attachLine, new LineSnapshot(null, attachLine.start, attachLine.end,
          new ArrayList<>(attachLine.waypoints), movedOf(involved, movedBoxes)));
      }
    }
  }

  /** A line is affected if one of the boxes it joins is moved, and not all of them are. */
  private static boolean isAffected(List<AbstractBoxModel_V001> involved, Collection<AbstractBoxModel_V001> moved) {
    boolean anyMoved = involved.stream().anyMatch(moved::contains);
    boolean allMoved = involved.stream().allMatch(moved::contains);
    return anyMoved && !allMoved;
  }

  private static List<AbstractBoxModel_V001> movedOf(List<AbstractBoxModel_V001> involved,
                                                    Collection<AbstractBoxModel_V001> moved) {
    return involved.stream().filter(moved::contains).toList();
  }

  private List<AbstractBoxModel_V001> involvedBoxes(AbstractRelationModel_V001 relation) {
    List<AbstractBoxModel_V001> boxes = new ArrayList<>();
    boxes.add(owners.boxOf(relation.fromId));
    boxes.add(owners.boxOf(relation.toId));
    boxes.removeIf(Objects::isNull);
    return boxes;
  }

  /** The box a line starts at and what it leads to: a box, or the boxes at the ends of a relation. */
  private List<AbstractBoxModel_V001> involvedBoxes(AttachLineModel_V001 attachLine) {
    List<AbstractBoxModel_V001> boxes = new ArrayList<>();
    boxes.add(owners.boxOf(attachLine.startId));
    AbstractRelationModel_V001 targetRelation = owners.relationWithId(attachLine.targetId);
    if (targetRelation != null) {
      boxes.addAll(involvedBoxes(targetRelation));
    }
    else {
      boxes.add(owners.boxOf(attachLine.targetId));
    }
    boxes.removeIf(Objects::isNull);
    return boxes;
  }

  /** Puts the waypoints and ends of all affected lines back as they were when the move started. */
  void reset() {
    startOfRelations.forEach((relation, start) -> relation.route = start.route);
    startOfAttachLines.forEach((attachLine, start) -> {
      attachLine.start = start.startAnchor;
      attachLine.end = start.endAnchor;
      attachLine.waypoints.clear();
      attachLine.waypoints.addAll(start.waypoints);
    });
  }

  /** Lets moved boxes eat the waypoints they lie over, then fixes the ends which would run through their box where the
   * lines enter it. The scene has to be laid out with the lines as they were when the move started, so after
   * {@link #reset}.
   * @param layout lays out the diagram again, which is needed once the lines have changed
   * @return the scene which shows the lines as they are now */
  Scene apply(Scene scene, Supplier<Scene> layout) {
    Scene result = scene;
    if (eatWaypoints(result)) {
      result = layout.get();
    }
    if (repairEnds(result)) {
      result = layout.get();
    }
    return result;
  }

  private boolean eatWaypoints(Scene scene) {
    boolean eaten = false;
    for (Map.Entry<AbstractRelationModel_V001, LineSnapshot> entry : startOfRelations.entrySet()) {
      AbstractRelationModel_V001 relation = entry.getKey();
      RouteModel_V001 route = relation.route;
      List<Rectangle2D> eaters = areasOf(entry.getValue().eatingBoxes, scene);
      List<PositionModel_V001> remaining = route.waypoints.stream().filter(waypoint -> !isCovered(waypoint, eaters)).toList();
      if (remaining.size() != route.waypoints.size()) {
        relation.route = new RouteModel_V001(route.start, remaining, route.end);
        eaten = true;
      }
    }
    for (Map.Entry<AttachLineModel_V001, LineSnapshot> entry : startOfAttachLines.entrySet()) {
      List<Rectangle2D> eaters = areasOf(entry.getValue().eatingBoxes, scene);
      eaten |= entry.getKey().waypoints.removeIf(waypoint -> isCovered(waypoint, eaters));
    }
    return eaten;
  }

  private static List<Rectangle2D> areasOf(List<AbstractBoxModel_V001> boxes, Scene scene) {
    List<Rectangle2D> areas = new ArrayList<>();
    for (AbstractBoxModel_V001 box : boxes) {
      SceneElementArea area = scene.elementAreaOf(box);
      if (area != null) {
        areas.add(area.area);
      }
    }
    return areas;
  }

  private static boolean isCovered(PositionModel_V001 waypoint, List<Rectangle2D> areas) {
    return areas.stream().anyMatch(area -> area.contains(waypoint.x, waypoint.y));
  }

  private boolean repairEnds(Scene scene) {
    boolean changed = false;
    for (AbstractRelationModel_V001 relation : startOfRelations.keySet()) {
      changed |= repair(relation, scene);
    }
    for (AttachLineModel_V001 attachLine : startOfAttachLines.keySet()) {
      changed |= repair(attachLine, scene);
    }
    return changed;
  }

  private boolean repair(AbstractRelationModel_V001 relation, Scene scene) {
    SceneConnectionLine line = scene.relationLineOf(relation);
    if (line == null) {
      return false;
    }
    List<Point2D> points = line.points();
    RouteModel_V001 route = relation.route;
    AnchorModel_V001 start = repaired(route.start, points.get(0), points.get(1), scene);
    AnchorModel_V001 end = repaired(route.end, points.get(points.size() - 1), points.get(points.size() - 2), scene);
    if (start == route.start && end == route.end) {
      return false;
    }
    relation.route = new RouteModel_V001(start, route.waypoints, end);
    return true;
  }

  private boolean repair(AttachLineModel_V001 attachLine, Scene scene) {
    SceneConnectionLine line = scene.attachLineOf(attachLine);
    if (line == null) {
      return false;
    }
    List<Point2D> points = line.points();
    AnchorModel_V001 start = repaired(attachLine.start, points.get(0), points.get(1), scene);
    AnchorModel_V001 end = repaired(attachLine.end, points.get(points.size() - 1), points.get(points.size() - 2), scene);
    if (start == attachLine.start && end == attachLine.end) {
      return false;
    }
    attachLine.start = start;
    attachLine.end = end;
    return true;
  }

  /** @param endPoint where the line ends on its box now
   * @param neighbor the next point of the line, a waypoint or the other end
   * @return the anchor itself if the line leaves the box properly, or an anchor where the line enters the box */
  private AnchorModel_V001 repaired(AnchorModel_V001 anchor, Point2D endPoint, Point2D neighbor, Scene scene) {
    if (anchor == null || anchor.center || anchor.isOnLine()) {
      return anchor;
    }
    AbstractBoxModel_V001 box = model.boxWithId(anchor.classId);
    SceneElementArea boxArea = box != null ? scene.elementAreaOf(box) : null;
    if (boxArea == null) {
      return anchor;
    }
    Rectangle2D area = boxArea.area;
    Point2D entry = BoxGeometry.entryIntoInterior(area, neighbor, endPoint);
    return entry != null ? BoxGeometry.nearestAnchor(anchor.classId, area, entry) : anchor;
  }
}
