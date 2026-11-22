package classman.editor;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.DiagramStyle;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorLineSelectionTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String ANCHORED = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  note (33333333) at(10, 200) \"remember\"\n  anchor 33333333 -- 11111111\n}";

  private static final String TWO_CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(400, 20)\n  note (33333333) at(10, 200) \"remember\"\n  anchor 33333333 -- 11111111\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
  }

  private Point2D middleOfLine() {
    List<Point2D> points = editing.scene().anchorLines.get(0).points();
    return new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
  }

  private void select(Point2D at) {
    control.pressed(at);
    control.released(at);
  }

  private AnchorLineModel_V001 anchorLine() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).anchorLines().get(0);
  }

  /** A press on the dashed line selects the anchor line, drawn thicker, with no box selected and no handles yet. */
  @Test
  void pressSelectsAnchor() throws ClassDiagramParseException {
    open(ANCHORED);

    select(middleOfLine());

    assertTrue(editing.hasSelection());
    assertNull(editing.selectedNote());
    assertNull(editing.selectedArea());
    assertEquals(2, editing.selectedHandles().size());
    assertEquals(DiagramStyle.SELECTED_LINE_WIDTH, editing.scene().anchorLines.get(0).line.strokeWidth);
  }

  /** A double click on the selected line adds a waypoint, on the waypoint it takes it away: the line is straight again. */
  @Test
  void waypointAddedAndDeleted() throws ClassDiagramParseException {
    open(ANCHORED);
    Point2D middle = middleOfLine();
    select(middle);

    control.doubleClicked(middle);

    assertEquals(1, anchorLine().waypoints.size());
    assertEquals(3, editing.selectedHandles().size());
    assertEquals(1, host.reportedSources.size());

    control.doubleClicked(editing.selectedHandles().get(1));

    assertTrue(anchorLine().waypoints.isEmpty());
    assertEquals(2, editing.selectedHandles().size());
    assertEquals(2, host.reportedSources.size());
  }

  /** A waypoint is dragged with its handle, in one step, and Esc puts it back. */
  @Test
  void waypointDragged() throws ClassDiagramParseException {
    open(ANCHORED);
    Point2D middle = middleOfLine();
    select(middle);
    control.doubleClicked(middle);
    Point2D handle = editing.selectedHandles().get(1);
    Point2D away = new Point2D.Double(handle.getX() + 100, handle.getY() + 60);

    control.pressed(handle);
    control.dragged(away);
    control.released(away);

    assertEquals(Math.round(away.getX()), anchorLine().waypoints.get(0).x);
    assertEquals(Math.round(away.getY()), anchorLine().waypoints.get(0).y);
    assertEquals(2, host.reportedSources.size());

    Point2D movedHandle = editing.selectedHandles().get(1);
    control.pressed(movedHandle);
    control.dragged(new Point2D.Double(movedHandle.getX() + 50, movedHandle.getY()));
    assertTrue(editing.cancelDrag());

    assertEquals(Math.round(away.getX()), anchorLine().waypoints.get(0).x);
  }

  /** The start handle slides along the border of the note and fixes the start of the line there, the end stays where
   * the layout puts it. */
  @Test
  void startSlidesAlongNote() throws ClassDiagramParseException {
    open(ANCHORED);
    select(middleOfLine());
    Point2D start = editing.selectedHandles().get(0);
    Point2D along = new Point2D.Double(start.getX() + 20, start.getY());

    control.pressed(start);
    control.dragged(along);
    control.released(along);

    assertEquals("33333333", anchorLine().start.classId);
    assertEquals(classman.model.v001.Side_V001.TOP, anchorLine().start.side);
    assertNull(anchorLine().end);
    assertEquals(1, host.reportedSources.size());
  }

  /** The end handle hangs the anchor line onto another class, framed while the mouse is on it. Esc puts it back. */
  @Test
  void endHungOntoOtherClass() throws ClassDiagramParseException {
    open(TWO_CLASSES);
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);
    Point2D onB = new Point2D.Double(editing.scene().elementAreas.get(1).area.getCenterX(),
      editing.scene().elementAreas.get(1).area.getCenterY());

    control.pressed(end);
    control.dragged(onB);

    assertEquals(editing.scene().elementAreas.get(1).area, editing.connectionTargetArea());
    assertTrue(editing.cancelDrag());
    assertEquals("11111111", anchorLine().targetId);

    control.pressed(end);
    control.dragged(onB);
    control.released(onB);

    assertEquals("22222222", anchorLine().targetId);
    assertEquals("22222222", anchorLine().end.classId);
  }

  /** While changes are tracked, the end of an existing line slides on its class but isn't hung onto another. */
  @Test
  void trackedEndStaysOnItsClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(TWO_CLASSES);
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);
    Point2D onB = new Point2D.Double(editing.scene().elementAreas.get(1).area.getCenterX(),
      editing.scene().elementAreas.get(1).area.getCenterY());

    control.pressed(end);
    control.dragged(onB);
    control.released(onB);

    assertEquals("11111111", anchorLine().targetId);
    assertEquals("11111111", anchorLine().end.classId);
  }

  /** While changes are tracked, the end of a line which a change set has added is hung onto another class. */
  @Test
  void trackedAddedLineEndIsHungOntoOtherClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(TWO_CLASSES.replace("anchor 33333333", "anchor change(added, yellow) 33333333"));
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);
    Point2D onB = new Point2D.Double(editing.scene().elementAreas.get(1).area.getCenterX(),
      editing.scene().elementAreas.get(1).area.getCenterY());

    control.pressed(end);
    control.dragged(onB);
    control.released(onB);

    assertEquals("22222222", anchorLine().targetId);
  }

  /** The Delete key takes the anchor line away with its waypoints, in one step, and selects nothing. */
  @Test
  void anchorDeleted() throws ClassDiagramParseException {
    open(ANCHORED);
    Point2D middle = middleOfLine();
    select(middle);
    control.doubleClicked(middle);

    editing.deleteSelected();

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).anchorLines().isEmpty());
    assertFalse(editing.hasSelection());
    assertEquals(2, host.reportedSources.size());
    assertTrue(editing.scene().anchorLines.isEmpty());
  }

  /** While changes are tracked, a line which a change set has added goes at once. */
  @Test
  void trackedAddedLineIsRemoved() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(ANCHORED.replace("anchor 33333333", "anchor change(added, yellow) 33333333"));
    select(middleOfLine());

    editing.deleteSelected();

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).anchorLines().isEmpty());
    assertFalse(editing.hasSelection());
  }

  /** While changes are tracked, any other line stays but is marked as removed, so that deleting it can be rejected. */
  @Test
  void trackedExistingLineIsMarkedRemoved() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(ANCHORED);
    select(middleOfLine());

    editing.deleteSelected();

    assertEquals(ChangeType_V001.REMOVED, anchorLine().change.type);
    assertEquals("yellow", anchorLine().change.changeSet);
    assertEquals(1, editing.scene().anchorLines.size());
    assertEquals(1, host.reportedSources.size());
  }
}
