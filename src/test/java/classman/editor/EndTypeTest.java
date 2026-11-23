package classman.editor;

import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.EndType_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
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

/** What there is at the ends of a relation (arrowhead, nothing or diamond), which the menu of an end handle changes. */
class EndTypeTest {
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

  /** A right click on an end handle asks for the menu of the end, one on a waypoint doesn't. */
  @Test
  void menuRequestedOnEndHandles() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");
    AtomicInteger shown = new AtomicInteger();
    PointerControl control = new PointerControl(editing, () -> { }, text -> { }, at -> shown.incrementAndGet(),
      pixels -> pixels);

    control.contextMenuRequested(editing.selectedHandles().get(0));
    assertEquals(1, shown.get());
    assertTrue(editing.hasSelection());

    control.contextMenuRequested(new Point2D.Double(200, 300));
    assertEquals(1, shown.get());
  }

  /** An association is navigable at the end where it has the arrowhead. The user can choose the other types there, but
   * not the one it has. */
  @Test
  void associationEnds() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");

    assertEquals(EndType_V001.ASSOCIATION, editing.endTypeAt(RelationEnd_V001.FROM));
    assertEquals(EndType_V001.NAVIGABLE, editing.endTypeAt(RelationEnd_V001.TO));
    assertFalse(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.NAVIGABLE).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.ASSOCIATION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.COMPOSITION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.AGGREGATION).can());
  }

  /** The arrowhead of an association is switched at both ends, one undo step each. */
  @Test
  void arrowheadsAtBothEnds() throws ClassDiagramParseException {
    openAndSelect("association 11111111 -> 22222222");

    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.NAVIGABLE);
    editing.setEndType(RelationEnd_V001.TO, EndType_V001.ASSOCIATION);

    assertTrue(relation().fromArrow);
    assertFalse(relation().toArrow);
    assertEquals(RelationType_V001.ASSOCIATION, relation().type);
    assertEquals(2, host.reportedSources.size());
    assertTrue(editing.canUndo());
  }

  /** A relation has one diamond at most: while an end has it, the other end can't get one, and the end which has it
   * can change from aggregation to composition. */
  @Test
  void oneDiamondAtMost() throws ClassDiagramParseException {
    openAndSelect("composition 11111111 -- 22222222");

    assertEquals(EndType_V001.COMPOSITION, editing.endTypeAt(RelationEnd_V001.FROM));
    assertFalse(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.COMPOSITION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.AGGREGATION).can());
    assertFalse(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.COMPOSITION).can());
    assertFalse(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.AGGREGATION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.NAVIGABLE).can());

    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.AGGREGATION);
    assertEquals(RelationType_V001.AGGREGATION, relation().type);

    editing.setEndType(RelationEnd_V001.TO, EndType_V001.COMPOSITION);
    assertEquals(RelationType_V001.AGGREGATION, relation().type);
    assertEquals(1, host.reportedSources.size());
  }

  /** The end which loses its diamond makes the relation an association, with or without an arrowhead there. */
  @Test
  void diamondTakenAway() throws ClassDiagramParseException {
    openAndSelect("composition 11111111 -- 22222222");

    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.NAVIGABLE);

    assertEquals(RelationType_V001.ASSOCIATION, relation().type);
    assertTrue(relation().fromArrow);

    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.COMPOSITION);
    assertEquals(RelationType_V001.COMPOSITION, relation().type);
    assertFalse(relation().fromArrow);

    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.ASSOCIATION);
    assertEquals(RelationType_V001.ASSOCIATION, relation().type);
    assertFalse(relation().fromArrow);
  }

  /** The diamond at the target lets the relation run the other way, and everything which belongs to an end, or is
   * measured from the start of the line, goes with it, so the diagram looks the same. */
  @Test
  void diamondAtTheTargetReversesTheRelation() throws ClassDiagramParseException {
    editing = new DiagramEditing(CLASSES.replace("  enum E (33333333) at(300, 200)\n",
      "  enum E (33333333) at(300, 200)\n  note (44444444) at(10, 300) \"n\"\n")
      + "  association (55555555) 11111111 1 at(14, 8) -> * at(20, -6) 22222222 \"owns\" at(40%, -12)\n"
      + "    route 11111111.bottom@10 (20, 150) (200, 150) 22222222.bottom@10\n"
      + "  anchor 44444444 -- 55555555 route 44444444.top@10 55555555@30%\n}", host);
    editing.selectAt(new Point2D.Double(20, 100));
    List<Point2D> handles = editing.selectedHandles();
    assertEquals(RelationEnd_V001.TO, editing.endHandleAt(handles.get(handles.size() - 1), TOLERANCE));

    editing.setEndType(RelationEnd_V001.TO, EndType_V001.COMPOSITION);

    var model = new ClassDiagramParser_V001().parse(editing.source());
    var reversed = (AssociationRelationModel_V001) model.relations().get(0);
    assertEquals(RelationType_V001.COMPOSITION, reversed.type);
    assertEquals("22222222", reversed.fromId);
    assertEquals("11111111", reversed.toId);
    assertEquals("*", reversed.fromMultiplicity);
    assertEquals("1", reversed.toMultiplicity);
    assertEquals(20, reversed.fromMultiplicityPlace.along);
    assertEquals(-6, -reversed.fromMultiplicityPlace.across);
    assertEquals(14, reversed.toMultiplicityPlace.along);
    assertEquals(60, reversed.labelPlace.along);
    assertEquals("owns", reversed.label);
    assertFalse(reversed.fromArrow);
    assertFalse(reversed.toArrow);
    assertEquals("22222222", reversed.route.start.classId);
    assertEquals("11111111", reversed.route.end.classId);
    assertEquals(200, reversed.route.waypoints.get(0).x);
    assertEquals(20, reversed.route.waypoints.get(1).x);
    assertEquals(70, model.anchorLines().get(0).end.offset);
  }

  /** An inheritance, a dependency and a relation to or from an enum have nothing to choose: no types are offered. */
  @Test
  void nothingToChooseForOtherRelations() throws ClassDiagramParseException {
    openAndSelect("extends 11111111 -> 22222222");
    assertFalse(editing.offersEndTypes());

    openAndSelect("association 11111111 -> 33333333");
    assertFalse(editing.offersEndTypes());

    openAndSelect("association 11111111 -> 22222222");
    assertTrue(editing.offersEndTypes());
  }

  /** Changing the types of the ends is no change which a change set tracks: it works the same while changes are
   * tracked, on every relation, and marks nothing. A removed relation can't be changed at all. */
  @Test
  void trackedChanges() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    openAndSelect("association 11111111 -> 22222222");
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.NAVIGABLE).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.COMPOSITION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.TO, EndType_V001.ASSOCIATION).can());
    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.NAVIGABLE);
    assertTrue(relation().fromArrow);
    assertNull(relation().change);
    editing.setEndType(RelationEnd_V001.FROM, EndType_V001.COMPOSITION);
    assertEquals(RelationType_V001.COMPOSITION, relation().type);
    assertNull(relation().change);

    openAndSelect("composition 11111111 -- 22222222");
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.ASSOCIATION).can());
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.AGGREGATION).can());

    openAndSelect("association (44444444) change(added, yellow) 11111111 -> 22222222");
    assertTrue(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.COMPOSITION).can());

    openAndSelect("association (44444444) change(removed, yellow) 11111111 -> 22222222");
    assertFalse(editing.canSetEndType(RelationEnd_V001.FROM, EndType_V001.NAVIGABLE).can());
  }
}
