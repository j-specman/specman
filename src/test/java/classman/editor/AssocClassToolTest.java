package classman.editor;

import classman.model.v001.AssocClassLinkModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
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

class AssocClassToolTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  class L (44444444) at(10, 200)\n  interface I (55555555) at(300, 200)\n";
  private static final String DIAGRAM = CLASSES + "  association (66666666) 11111111 -- 22222222\n}";

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

  private Point2D onRelation(int index) {
    List<Point2D> points = editing.scene().relationLines.get(index).points();
    Point2D first = points.get(0);
    Point2D last = points.get(points.size() - 1);
    return new Point2D.Double((first.getX() + last.getX()) / 2, (first.getY() + last.getY()) / 2);
  }

  private void draw(Point2D from, Point2D to) {
    control.pressed(from);
    control.dragged(to);
    control.released(to);
  }

  private ClassDiagramModel_V001 model() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }

  private AssocClassLinkModel_V001 link() throws ClassDiagramParseException {
    return model().assocClassLinks().get(0);
  }

  /** A press on a class and a drag onto a relation links the class to it, in one step, and the tool is left. */
  @Test
  void classLinkedToRelation() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(0));

    assertEquals("44444444", link().startId);
    assertEquals("66666666", link().targetId);
    assertNull(link().change);
    assertFalse(editing.isToolArmed());
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
    assertEquals("L", editing.selectedClass().name);
  }

  /** Neither an interface nor a note starts the link, nor does a relation, and released on no relation nothing happens
   * and the tool stays armed. */
  @Test
  void onlyClassToRelation() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAssocClassTool();

    draw(inBox(3), onRelation(0));
    draw(onRelation(0), inBox(2));
    draw(inBox(2), new Point2D.Double(200, 500));

    assertTrue(model().assocClassLinks().isEmpty());
    assertTrue(host.reportedSources.isEmpty());
    assertTrue(editing.isAssocClassToolArmed());
  }

  /** The line of an inheritance is no target. */
  @Test
  void inheritanceIsNoTarget() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(10, 300)\n"
      + "  class C (44444444) at(400, 100)\n  extends 22222222 -> 11111111\n}");
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(0));

    assertTrue(model().assocClassLinks().isEmpty());
    assertTrue(editing.isAssocClassToolArmed());
  }

  /** A relation has only one association class: it is no target for another class. */
  @Test
  void relationTakesOnlyOneClass() throws ClassDiagramParseException {
    open(DIAGRAM.replace("}", "  assocclass 44444444 -- 66666666\n}"));
    editing.toggleAssocClassTool();

    draw(inBox(1), onRelation(0));

    assertEquals(1, model().assocClassLinks().size());
    assertEquals("44444444", link().startId);
    assertTrue(host.reportedSources.isEmpty());
  }

  /** A class which the relation connects is not its association class. */
  @Test
  void endOfRelationIsNoAssocClass() throws ClassDiagramParseException {
    open(DIAGRAM);
    editing.toggleAssocClassTool();

    draw(inBox(0), onRelation(0));

    assertTrue(model().assocClassLinks().isEmpty());
    assertTrue(editing.isAssocClassToolArmed());
  }

  /** A class which is the association class of a relation is no start: the user deletes the link first. */
  @Test
  void classWithLinkIsNoStart() throws ClassDiagramParseException {
    open(CLASSES + "  association (66666666) 11111111 -- 22222222\n  association (77777777) 22222222 -- 55555555\n"
      + "  assocclass 44444444 -- 66666666\n}");
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(1));

    assertEquals(1, model().assocClassLinks().size());
    assertEquals("66666666", link().targetId);
    assertTrue(host.reportedSources.isEmpty());
    assertTrue(editing.isAssocClassToolArmed());
  }

  /** The link refers to the class and the relation by ID, so both get one. */
  @Test
  void classAndRelationWithoutIdGetOne() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n  class L at(10, 200)\n"
      + "  association 11111111 -- 22222222\n}");
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(0));

    ClassDiagramModel_V001 model = model();
    assertNotNull(model.classes().get(2).id);
    assertNotNull(model.relations().get(0).id);
    assertEquals(model.classes().get(2).id, link().startId);
    assertEquals(model.relations().get(0).id, link().targetId);
  }

  /** While changes are tracked, any class can be linked: the line is marked as added, the class has no mark. */
  @Test
  void trackedExistingClassGetsAddedLink() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(DIAGRAM);
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(0));

    assertEquals(ChangeType_V001.ADDED, link().change.type);
    assertEquals("yellow", link().change.changeSet);
    assertNull(model().classes().get(2).change);
  }

  /** A link which a change set removed frees its relation for another class. */
  @Test
  void removedLinkFreesRelation() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(CLASSES + "  class M (88888888) at(10, 400)\n  association (66666666) 11111111 -- 22222222\n"
      + "  assocclass change(removed, yellow) 44444444 -- 66666666\n}");
    editing.toggleAssocClassTool();

    draw(inBox(4), onRelation(0));

    assertEquals(2, model().assocClassLinks().size());
    assertEquals("88888888", model().assocClassLinks().get(1).startId);
    assertEquals(ChangeType_V001.ADDED, model().assocClassLinks().get(1).change.type);
  }

  /** A class whose link a change set removed is free again, for another relation. */
  @Test
  void removedLinkFreesClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(CLASSES + "  association (66666666) 11111111 -- 22222222\n  association (77777777) 22222222 -- 55555555\n"
      + "  assocclass change(removed, yellow) 44444444 -- 66666666\n}");
    editing.toggleAssocClassTool();

    draw(inBox(2), onRelation(1));

    assertEquals(2, model().assocClassLinks().size());
    assertEquals("77777777", model().assocClassLinks().get(1).targetId);
  }

  /** The link is selected by its line like an anchor line, and Delete takes only the line away: the class and the
   * relation stay. */
  @Test
  void deletedLinkLeavesClassAndRelation() throws ClassDiagramParseException {
    open(DIAGRAM.replace("}", "  assocclass 44444444 -- 66666666\n}"));
    List<Point2D> points = editing.scene().attachLines.get(0).points();
    editing.selectAt(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2));
    assertEquals(2, editing.selectedHandles().size());

    editing.deleteSelected();

    assertTrue(model().assocClassLinks().isEmpty());
    assertEquals(4, model().classes().size());
    assertEquals(1, model().relations().size());
  }

  /** The tools exclude each other. */
  @Test
  void toolsExcludeEachOther() throws ClassDiagramParseException {
    open(DIAGRAM);

    editing.toggleAnchorTool();
    editing.toggleAssocClassTool();

    assertTrue(editing.isAssocClassToolArmed());
    assertFalse(editing.isAnchorToolArmed());
  }
}
