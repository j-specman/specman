package classman.editor;

import classman.model.v001.NoteModel_V001;
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
    + "  note (33333333) at(10, 200) \"remember\" attach(11111111)\n}";

  private static final String TWO_CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(400, 20)\n  note (33333333) at(10, 200) \"remember\" attach(11111111)\n}";

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

  private NoteModel_V001 note() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).notes().get(0);
  }

  /** A press on the dashed line selects the anchor, drawn thicker, with no box selected and no handles yet. */
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

  /** A double click on the selected line adds a waypoint, on the waypoint it takes it away: the anchor is straight again. */
  @Test
  void waypointAddedAndDeleted() throws ClassDiagramParseException {
    open(ANCHORED);
    Point2D middle = middleOfLine();
    select(middle);

    control.doubleClicked(middle);

    assertEquals(1, note().attachWaypoints.size());
    assertEquals(3, editing.selectedHandles().size());
    assertEquals(1, host.reportedSources.size());

    control.doubleClicked(editing.selectedHandles().get(1));

    assertTrue(note().attachWaypoints.isEmpty());
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

    assertEquals(Math.round(away.getX()), note().attachWaypoints.get(0).x);
    assertEquals(Math.round(away.getY()), note().attachWaypoints.get(0).y);
    assertEquals(2, host.reportedSources.size());

    Point2D movedHandle = editing.selectedHandles().get(1);
    control.pressed(movedHandle);
    control.dragged(new Point2D.Double(movedHandle.getX() + 50, movedHandle.getY()));
    assertTrue(editing.cancelDrag());

    assertEquals(Math.round(away.getX()), note().attachWaypoints.get(0).x);
  }

  /** The start handle slides along the border of the note and fixes the start of the line there. The note gets an ID
   * for it, the end stays where the layout puts it. */
  @Test
  void startSlidesAlongNote() throws ClassDiagramParseException {
    open(ANCHORED);
    select(middleOfLine());
    Point2D start = editing.selectedHandles().get(0);
    Point2D along = new Point2D.Double(start.getX() + 20, start.getY());

    control.pressed(start);
    control.dragged(along);
    control.released(along);

    assertEquals(note().id, note().attachStart.classId);
    assertEquals(classman.model.v001.Side_V001.TOP, note().attachStart.side);
    assertNull(note().attachEnd);
    assertEquals(1, host.reportedSources.size());
  }

  /** The end handle hangs the anchor onto another class, framed while the mouse is on it. Esc puts it back. */
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
    assertEquals("11111111", note().attachedToId);

    control.pressed(end);
    control.dragged(onB);
    control.released(onB);

    assertEquals("22222222", note().attachedToId);
    assertEquals("22222222", note().attachEnd.classId);
  }

  /** While changes are tracked, the end of the anchor of an existing note slides on its class but isn't hung onto another. */
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

    assertEquals("11111111", note().attachedToId);
    assertEquals("11111111", note().attachEnd.classId);
  }

  /** The Delete key takes the anchor away with its waypoints, in one step, and selects nothing. */
  @Test
  void anchorDeleted() throws ClassDiagramParseException {
    open(ANCHORED);
    Point2D middle = middleOfLine();
    select(middle);
    control.doubleClicked(middle);

    editing.deleteSelected();

    assertNull(note().attachedToId);
    assertTrue(note().attachWaypoints.isEmpty());
    assertFalse(editing.hasSelection());
    assertEquals(2, host.reportedSources.size());
    assertTrue(editing.scene().anchorLines.isEmpty());
  }

  /** While changes are tracked, the anchor of an existing note stays, the one of a note which was added can go. */
  @Test
  void trackedOnlyAddedNotesLoseAnchor() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"old\" attach(11111111)\n"
      + "  note (44444444) change(added, yellow) at(10, 400) \"new\" attach(11111111)\n}");
    Point2D oldLine = middleOfLine();
    select(oldLine);

    editing.deleteSelected();

    assertTrue(host.reportedSources.isEmpty());
    assertTrue(editing.hasSelection());

    List<Point2D> points = editing.scene().anchorLines.get(1).points();
    select(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2));
    editing.deleteSelected();

    assertEquals(1, editing.scene().anchorLines.size());
    assertEquals(1, host.reportedSources.size());
  }
}
