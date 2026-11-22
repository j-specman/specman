package classman.editor;

import classman.model.v001.RelationType_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
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

  private ClassDiagramModel_V001 model() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }

  private AnchorLineModel_V001 anchorLine() throws ClassDiagramParseException {
    return model().anchorLines().get(0);
  }

  /** A press on a note and a drag to a class draws an anchor line from the note to it, in one step, and the tool is left. */
  @Test
  void noteAnchoredToClass() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(0));

    assertEquals("33333333", anchorLine().startId);
    assertEquals("11111111", anchorLine().targetId);
    assertNull(anchorLine().change);
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

    assertTrue(model().anchorLines().isEmpty());
    assertTrue(host.reportedSources.isEmpty());
    assertTrue(editing.isAnchorToolArmed());
  }

  /** A note is attached to another class by anchoring it again: the line leads there, without the waypoints of the old one. */
  @Test
  void anchorMovedToOtherClass() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n"
      + "  note (33333333) at(10, 200) \"remember\"\n  anchor 33333333 -- 11111111 route (150, 150)\n}");
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(1));

    assertEquals(1, model().anchorLines().size());
    assertEquals("22222222", anchorLine().targetId);
    assertTrue(anchorLine().waypoints.isEmpty());
  }

  /** Anchoring a note to the class it hangs on is no operation. */
  @Test
  void sameClassIsNoOperation() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"n\"\n"
      + "  anchor 33333333 -- 11111111\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** A class without an ID gets one, because the anchor line refers to it by ID, and so does a note without one. */
  @Test
  void classAndNoteWithoutIdGetOne() throws ClassDiagramParseException {
    open("classdiagram {\n  class A at(10, 20)\n  note at(10, 200) \"n\"\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    ClassDiagramModel_V001 model = model();
    assertNotNull(model.classes().get(0).id);
    assertNotNull(model.notes().get(0).id);
    assertEquals(model.classes().get(0).id, anchorLine().targetId);
    assertEquals(model.notes().get(0).id, anchorLine().startId);
  }

  /** While changes are tracked, a note which exists can get an anchor line too: it is marked as added. */
  @Test
  void trackedExistingNoteGetsAddedLine() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"old\"\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    assertEquals(ChangeType_V001.ADDED, anchorLine().change.type);
    assertEquals("yellow", anchorLine().change.changeSet);
    assertNull(model().notes().get(0).change);
  }

  /** While changes are tracked, a new target for a note with a line is a removed line and an added one, like a relation
   * which is deleted and drawn again. */
  @Test
  void trackedNewTargetRemovesAndAdds() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n"
      + "  note (33333333) at(10, 200) \"old\"\n  anchor 33333333 -- 11111111\n}");
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(1));

    assertEquals(2, model().anchorLines().size());
    AnchorLineModel_V001 old = model().anchorLines().get(0);
    AnchorLineModel_V001 added = model().anchorLines().get(1);
    assertEquals("11111111", old.targetId);
    assertEquals(ChangeType_V001.REMOVED, old.change.type);
    assertEquals("22222222", added.targetId);
    assertEquals(ChangeType_V001.ADDED, added.change.type);
    assertEquals(1, host.reportedSources.size());
  }

  /** A line which a change set has added is given a new target in place. */
  @Test
  void trackedAddedLineIsRetargeted() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n"
      + "  note (33333333) at(10, 200) \"old\"\n  anchor change(added, yellow) 33333333 -- 11111111\n}");
    editing.toggleAnchorTool();

    draw(inBox(2), inBox(1));

    assertEquals(1, model().anchorLines().size());
    assertEquals("22222222", anchorLine().targetId);
    assertEquals(ChangeType_V001.ADDED, anchorLine().change.type);
  }

  /** A note whose line is removed is attached again by a new added line, also to the same class. */
  @Test
  void trackedRemovedLineDoesNotCount() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  note (33333333) at(10, 200) \"old\"\n"
      + "  anchor change(removed, yellow) 33333333 -- 11111111\n}");
    editing.toggleAnchorTool();

    draw(inBox(1), inBox(0));

    assertEquals(2, model().anchorLines().size());
    assertEquals(ChangeType_V001.ADDED, model().anchorLines().get(1).change.type);
  }

  /** The two tools exclude each other, and the cursor is a crosshair while one is armed. */
  @Test
  void toolsExcludeEachOther() throws ClassDiagramParseException {
    open(DIAGRAM);

    editing.toggleRelationTool(RelationType_V001.ASSOCIATION);
    editing.toggleAnchorTool();

    assertTrue(editing.isAnchorToolArmed());
    assertFalse(editing.isRelationToolArmed());
    assertEquals(java.awt.Cursor.CROSSHAIR_CURSOR, control.cursorAt(inBox(2)).getType());
  }
}
