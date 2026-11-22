package classman.editor;

import classman.model.v001.NoteModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorToolTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  note (33333333) at(10, 200) \"remember\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
  }

  private Point2D inBox(int index) {
    Rectangle2D area = editing.scene().elementAreas.get(index).area;
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private void draw(Point2D from, Point2D to) {
    control.pressed(from);
    control.dragged(to);
    control.released(to);
  }

  private NoteModel_V001 note() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).notes().get(0);
  }

  /** A press on a note and a drag to a class attaches the note to it, in one step, and the tool is left. */
  @Test
  void noteAnchoredToClass() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(0));

    assertEquals("11111111", note().attachedToId);
    assertFalse(editing.isToolArmed());
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
    assertNotNull(editing.selectedNote());
    assertEquals(1, editing.scene().elementAreas.stream().filter(area -> area.element == editing.selectedNote()).count());
  }

  /** Released on no class, nothing happens and the tool stays armed; a press on a class starts nothing. */
  @Test
  void onlyNoteToClass() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAnchorTool();

    draw(inBox(2), new Point2D.Double(200, 500));
    draw(inBox(0), inBox(1));

    assertNull(note().attachedToId);
    assertTrue(host.reportedSources.isEmpty());
    assertTrue(editing.isAnchorToolArmed());
  }

  /** A note is attached to another class by anchoring it again, and the dashed line loses its waypoints. */
  @Test
  void anchorMovedToOtherClass() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n"
      + "  note (33333333) at(10, 200) \"remember\" attach(11111111) route (150, 150)\n}");
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(1));

    assertEquals("22222222", note().attachedToId);
    assertTrue(note().attachWaypoints.isEmpty());
  }

  /** Anchoring a note to the class it hangs on is no operation. */
  @Test
  void sameClassIsNoOperation() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"n\" attach(11111111)\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** A class without an ID gets one, because the note refers to it by ID. */
  @Test
  void classWithoutIdGetsOne() throws ClassDiagramParseException {
    open("classdiagram {\n  class A at(10, 20)\n  note (33333333) at(10, 200) \"n\"\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    String classId = new ClassDiagramParser_V001().parse(editing.source()).classes().get(0).id;
    assertNotNull(classId);
    assertEquals(classId, note().attachedToId);
  }

  /** While changes are tracked, a note which exists can't get an anchor, but one which a change set has added can. */
  @Test
  void trackedOnlyAddedNotes() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"old\"\n"
      + "  note (44444444) change(added, yellow) at(10, 300) \"new\"\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));
    assertTrue(host.reportedSources.isEmpty());

    draw(inBox(2), inBox(0));
    assertEquals("11111111", new ClassDiagramParser_V001().parse(editing.source()).notes().get(1).attachedToId);
  }

  /** The two tools exclude each other, and the cursor is a crosshair while one is armed. */
  @Test
  void toolsExcludeEachOther() throws ClassDiagramParseException {
    open(DIAGRAM);

    editing.toggleRelationTool();
    editing.toggleAnchorTool();

    assertTrue(editing.isAnchorToolArmed());
    assertFalse(editing.isRelationToolArmed());
    assertEquals(java.awt.Cursor.CROSSHAIR_CURSOR, control.cursorAt(inBox(2)).getType());
  }
}
