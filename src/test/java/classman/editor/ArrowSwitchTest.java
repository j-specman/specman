package classman.editor;

import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The arrowheads at the ends of a relation, which the menu of an end handle switches. */
class ArrowSwitchTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  enum E (33333333) at(300, 200)\n";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void openAndSelect(String relation) throws ClassDiagramParseException {
    editing = new DiagramEditing(CLASSES + relation + "\n}", host);
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2));
    assertTrue(editing.selectedHandles().size() >= 2, "the relation is selected");
  }

  private AssociationRelationModel_V001 relation() throws ClassDiagramParseException {
    return (AssociationRelationModel_V001) new ClassDiagramParser_V001().parse(editing.source()).relations().get(0);
  }

  /** The handles at the start and at the end of the line tell which end a menu is for. */
  @Test
  void endHandles() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");
    List<Point2D> handles = editing.selectedHandles();

    assertEquals(RelationEnd_V001.FROM, editing.endHandleAt(handles.get(0), TOLERANCE));
    assertEquals(RelationEnd_V001.TO, editing.endHandleAt(handles.get(handles.size() - 1), TOLERANCE));
    assertNull(editing.endHandleAt(new Point2D.Double(200, 300), TOLERANCE));
    editing.deselect();
    assertNull(editing.endHandleAt(handles.get(0), TOLERANCE));
  }

  /** The handle of a waypoint is no end. */
  @Test
  void waypointIsNoEnd() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222\n  route 11111111.bottom@10 (20, 150) 22222222.bottom@10");

    assertNull(editing.endHandleAt(editing.selectedHandles().get(1), TOLERANCE));
  }

  /** The arrowhead of an association can be switched at both ends, one undo step each. */
  @Test
  void associationBothEnds() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");
    assertTrue(editing.hasArrowAt(RelationEnd_V001.TO));
    assertFalse(editing.hasArrowAt(RelationEnd_V001.FROM));

    editing.toggleArrowAt(RelationEnd_V001.FROM);
    editing.toggleArrowAt(RelationEnd_V001.TO);

    assertTrue(relation().fromArrow);
    assertFalse(relation().toArrow);
    assertEquals(2, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** The end without the diamond of an aggregation or composition can switch, the diamond end can't. */
  @Test
  void diamondEndCantSwitch() throws ClassDiagramParseException {
    openAndSelect("composition 11111111 -- 22222222");

    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.FROM));
    assertTrue(editing.canSwitchArrowAt(RelationEnd_V001.TO));
    editing.toggleArrowAt(RelationEnd_V001.FROM);
    assertFalse(relation().fromArrow);
    editing.toggleArrowAt(RelationEnd_V001.TO);
    assertTrue(relation().toArrow);
  }

  /** An inheritance has no arrowheads to switch. */
  @Test
  void inheritanceCantSwitch() throws ClassDiagramParseException {
    openAndSelect("extends 11111111 -> 22222222");

    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.FROM));
    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.TO));
  }

  /** A relation to an enum, or from one, keeps its arrowheads: the enum doesn't know who uses it. */
  @Test
  void enumRelationsCantSwitch() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 33333333");

    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.FROM));
    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.TO));
    editing.toggleArrowAt(RelationEnd_V001.TO);
    assertTrue(relation().toArrow);
  }

  /** The switch is no change which a change set tracks: it works while changes are tracked, also for a relation which no
   * change set has added, and marks nothing. A removed relation can't be switched. */
  @Test
  void trackedNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    openAndSelect("association 11111111 -> 22222222");
    assertTrue(editing.canSwitchArrowAt(RelationEnd_V001.FROM));
    editing.toggleArrowAt(RelationEnd_V001.FROM);
    assertTrue(relation().fromArrow);
    assertNull(relation().change);

    openAndSelect("association (44444444) change(added, yellow) 11111111 -> 22222222");
    assertTrue(editing.canSwitchArrowAt(RelationEnd_V001.FROM));

    openAndSelect("association (44444444) change(removed, yellow) 11111111 -> 22222222");
    assertFalse(editing.canSwitchArrowAt(RelationEnd_V001.FROM));
  }

  /** A right click on an end handle asks for the menu of the end, one on a waypoint doesn't. */
  @Test
  void menuRequestedOnEndHandles() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");
    AtomicInteger shown = new AtomicInteger();
    PointerControl control = new PointerControl(editing, () -> { }, text -> { }, at -> shown.incrementAndGet(),
      pixels -> pixels);

    control.contextMenuRequested(editing.selectedHandles().get(0));
    assertEquals(1, shown.get());
    assertEquals(1, editing.selectedHandles().size() > 0 ? 1 : 0, "the relation stays selected");
    assertTrue(editing.hasSelection());

    control.contextMenuRequested(new Point2D.Double(200, 300));
    assertEquals(1, shown.get());
  }
}
