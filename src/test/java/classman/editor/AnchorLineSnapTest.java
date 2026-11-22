package classman.editor;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnchorLineSnapTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  /** The line leaves the small note at its bottom edge, because the waypoint is far below it. */
  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  note (33333333) at(300, 100) \"eins\"\n  anchor 33333333 -- 11111111 route (250, 400)\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;
  private Rectangle2D note;

  private void open() throws ClassDiagramParseException {
    editing = new DiagramEditing(DIAGRAM, host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
    note = editing.scene().elementAreas.stream().filter(area -> area.element instanceof NoteModel_V001)
      .findFirst().orElseThrow().area;
    List<Point2D> points = editing.scene().attachLines.get(0).points();
    Point2D middle = new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
    control.pressed(middle);
    control.released(middle);
  }

  private AnchorLineModel_V001 dragWaypointTo(double x, double y) throws ClassDiagramParseException {
    Point2D waypoint = editing.selectedHandles().get(1);
    Point2D to = new Point2D.Double(x, y);
    control.pressed(waypoint);
    control.dragged(to);
    control.released(to);
    return new ClassDiagramParser_V001().parse(editing.source()).anchorLines().get(0);
  }

  /** The end of the line which isn't fixed moves with the waypoint, so the height where it was when the drag started
   * is no height to snap to: a waypoint just below the bottom edge of the note stays where it is dragged to. */
  @Test
  void noSnapToStaleStartHeight() throws ClassDiagramParseException {
    open();

    AnchorLineModel_V001 line = dragWaypointTo(250, note.getMaxY() + 3);

    assertEquals(Math.round(note.getMaxY() + 3), line.waypoints.get(0).y);
  }

  /** The waypoint snaps to the height of the middle of the note, where the line to the note runs horizontally. */
  @Test
  void snapsToMiddleOfNote() throws ClassDiagramParseException {
    open();

    AnchorLineModel_V001 line = dragWaypointTo(250, note.getCenterY() + 3);

    assertEquals(Math.round(note.getCenterY()), line.waypoints.get(0).y);
  }

  /** It snaps to the middle of the class above it, where the line to the class runs vertically. */
  @Test
  void snapsToMiddleOfTargetClass() throws ClassDiagramParseException {
    open();
    Rectangle2D target = editing.scene().elementAreas.get(0).area;

    AnchorLineModel_V001 line = dragWaypointTo(target.getCenterX() + 3, 400);

    assertEquals(Math.round(target.getCenterX()), line.waypoints.get(0).x);
  }
}
