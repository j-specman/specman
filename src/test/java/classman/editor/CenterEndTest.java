package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Linking an end of a relation to the center of its class again, after it was fixed on the border. */
class CenterEndTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n";
  private static final String ROUTED = CLASSES + "  association 11111111 -- 22222222\n"
    + "  route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void openAndSelect(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    int segment = points.size() > 3 ? 1 : 0;
    editing.selectAt(new Point2D.Double((points.get(segment).getX() + points.get(segment + 1).getX()) / 2,
      (points.get(segment).getY() + points.get(segment + 1).getY()) / 2));
    assertTrue(editing.hasSelection(), "the relation is selected");
  }

  private AbstractRelationModel_V001 relation() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).relations().get(0);
  }

  /** The end of a relation with a route, which is fixed on a side, can be linked to the center again; the other end
   * stays where it is. */
  @Test
  void endLinkedToCenter() throws ClassDiagramParseException {
    openAndSelect(ROUTED);
    Point2D other = editing.selectedHandles().get(editing.selectedHandles().size() - 1);
    assertTrue(editing.canCenterEnd(RelationEnd_V001.FROM).can());

    editing.centerEnd(RelationEnd_V001.FROM);

    assertTrue(relation().route.start.center);
    assertFalse(relation().route.end.center);
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    assertEquals(other, points.get(points.size() - 1));
    assertFalse(editing.canCenterEnd(RelationEnd_V001.FROM).can());
    assertTrue(editing.canCenterEnd(RelationEnd_V001.TO).can());
    assertEquals(1, host.reportedSources.size());
  }

  /** The linked end leaves the class where a line from its center to the next point crosses the border. */
  @Test
  void linkedEndPointsAtTheNextPoint() throws ClassDiagramParseException {
    openAndSelect(ROUTED);

    editing.centerEnd(RelationEnd_V001.FROM);

    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    Point2D start = editing.scene().relationLines.get(0).points().get(0);
    // the first waypoint (20, 150) is below the class, nearly straight under its center
    assertEquals(area.getMaxY(), start.getY(), 0.01);
    assertTrue(start.getX() > area.getMinX() && start.getX() < area.getMaxX());
  }

  /** With both ends linked and no waypoint left the relation is a straight line again, without a route. */
  @Test
  void routeDroppedWhenStraight() throws ClassDiagramParseException {
    openAndSelect(CLASSES + "  association 11111111 -- 22222222\n  route 11111111.right@10 22222222.left@10\n}");

    editing.centerEnd(RelationEnd_V001.FROM);
    assertNotNull(relation().route);
    editing.centerEnd(RelationEnd_V001.TO);

    assertNull(relation().route);
  }

  /** A relation without a route has nothing to link, and the change is no change which a change set tracks. */
  @Test
  void straightRelationNothingToLink() throws ClassDiagramParseException {
    openAndSelect(CLASSES + "  association 11111111 -- 22222222\n}");

    assertFalse(editing.canCenterEnd(RelationEnd_V001.FROM).can());
    editing.centerEnd(RelationEnd_V001.FROM);
    assertFalse(editing.canUndo());
  }

  /** Linking is no change which a change set tracks: it works while changes are tracked and marks nothing. */
  @Test
  void trackedNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    openAndSelect(ROUTED);

    editing.centerEnd(RelationEnd_V001.TO);

    assertTrue(relation().route.end.center);
    assertNull(relation().change);
  }

  private static final String ANCHORED = CLASSES + "  note (44444444) at(10, 200) \"n\"\n"
    + "  anchor 44444444 -- 11111111\n  route 44444444.right@3 (200, 250) 11111111.bottom@5\n}";

  private void openAndSelectAnchorLine(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    List<Point2D> points = editing.scene().attachLines.get(0).points();
    editing.selectAt(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2));
    assertTrue(editing.hasSelection(), "the anchor line is selected");
  }

  private classman.model.v001.AnchorLineModel_V001 anchorLine() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).anchorLines().get(0);
  }

  /** The ends of an anchor line can be linked to the center of their boxes again, too: the layout works them out. */
  @Test
  void anchorLineEnds() throws ClassDiagramParseException {
    openAndSelectAnchorLine(ANCHORED);
    List<Point2D> handles = editing.selectedHandles();
    assertEquals(RelationEnd_V001.FROM, editing.endHandleAt(handles.get(0), 4));
    assertEquals(RelationEnd_V001.TO, editing.endHandleAt(handles.get(handles.size() - 1), 4));
    assertFalse(editing.isRelationSelected());
    assertTrue(editing.canCenterEnd(RelationEnd_V001.FROM).can());
    assertTrue(editing.canCenterEnd(RelationEnd_V001.TO).can());

    editing.centerEnd(RelationEnd_V001.FROM);

    assertNull(anchorLine().start);
    assertNotNull(anchorLine().end);
    assertEquals(1, anchorLine().waypoints.size());
    assertFalse(editing.canCenterEnd(RelationEnd_V001.FROM).can());
    assertEquals(1, host.reportedSources.size());

    editing.centerEnd(RelationEnd_V001.TO);
    assertNull(anchorLine().end);
  }

  /** Linking the end of an anchor line marks nothing, also while changes are tracked. */
  @Test
  void anchorLineTrackedNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    openAndSelectAnchorLine(ANCHORED);

    editing.centerEnd(RelationEnd_V001.TO);

    assertNull(anchorLine().end);
    assertNull(anchorLine().change);
  }
}
