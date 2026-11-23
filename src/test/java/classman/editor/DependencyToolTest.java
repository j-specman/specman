package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.DependencyRelationModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneRelationLine;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dependency tool draws a dashed line with an arrowhead from a class or attribute to another class or attribute. */
class DependencyToolTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final double TOLERANCE = 4;
  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20) {\n    name\n"
    + "    age (0a0a0a0a)\n  }\n  class B (22222222) at(400, 20) {\n    other (0b0b0b0b)\n    plain\n  }\n"
    + "  class C (33333333) at(400, 300)\n  note (44444444) at(10, 300) \"n\"\n";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String elements) throws ClassDiagramParseException {
    open(DIAGRAM, elements);
  }

  private void open(String diagram, String elements) throws ClassDiagramParseException {
    editing = new DiagramEditing(diagram + elements + "}", host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
  }

  private void openWithTool(String elements) throws ClassDiagramParseException {
    open(elements);
    editing.toggleDependencyTool();
  }

  private static Point2D center(Rectangle2D area) {
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private Point2D inClass(String name) {
    return center(editing.scene().elementAreas.stream()
      .filter(area -> area.element instanceof ClassModel_V001 box && name.equals(box.name)).findFirst().orElseThrow().area);
  }

  private Point2D aboveAttributes(String className) {
    Rectangle2D area = editing.scene().elementAreas.stream()
      .filter(candidate -> candidate.element instanceof ClassModel_V001 box && className.equals(box.name))
      .findFirst().orElseThrow().area;
    return new Point2D.Double(area.getCenterX(), area.getMinY() + 3);
  }

  private Point2D onAttribute(String name) {
    return center(editing.scene().textAreas.stream()
      .filter(text -> text.kind == TextKind.ATTRIBUTE_LINE && text.attribute != null && name.equals(text.attribute.name))
      .findFirst().orElseThrow().area);
  }

  private void draw(Point2D from, Point2D to) {
    control.pressed(from);
    control.dragged(to);
    control.released(to);
  }

  private ClassDiagramModel_V001 model() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source());
  }

  private DependencyRelationModel_V001 onlyDependency() throws ClassDiagramParseException {
    List<AbstractRelationModel_V001> relations = model().relations();
    assertEquals(1, relations.size());
    assertEquals(RelationType_V001.DEPENDENCY, relations.get(0).type);
    return (DependencyRelationModel_V001) relations.get(0);
  }

  // ---- drawing ----

  /** A press on a class and a drag to another one creates a dependency between them, selects it and leaves the tool. */
  @Test
  void classToClass() throws ClassDiagramParseException {
    openWithTool("");

    draw(aboveAttributes("A"), inClass("C"));

    DependencyRelationModel_V001 dependency = onlyDependency();
    assertEquals("11111111", dependency.fromId);
    assertEquals("33333333", dependency.toId);
    assertNotNull(dependency.id);
    assertNull(dependency.change);
    assertFalse(editing.isToolArmed());
    assertEquals(dependency.id, editing.selectedRelation().id);
    assertEquals(1, host.reportedSources.size());
  }

  /** An attribute which has an ID is the start, a class the end. */
  @Test
  void attributeToClass() throws ClassDiagramParseException {
    openWithTool("");

    draw(onAttribute("age"), inClass("C"));

    assertEquals("0a0a0a0a", onlyDependency().fromId);
    assertEquals("33333333", onlyDependency().toId);
  }

  /** The end can be an attribute too, which wins over its class where the mouse is on it. */
  @Test
  void classToAttribute() throws ClassDiagramParseException {
    openWithTool("");

    draw(inClass("C"), onAttribute("other"));

    assertEquals("33333333", onlyDependency().fromId);
    assertEquals("0b0b0b0b", onlyDependency().toId);
  }

  /** Attributes without an ID get one in the operation which creates the dependency. */
  @Test
  void attributesWithoutIdGetOne() throws ClassDiagramParseException {
    openWithTool("");

    draw(onAttribute("name"), onAttribute("plain"));

    DependencyRelationModel_V001 dependency = onlyDependency();
    assertNotNull(dependency.fromId);
    assertNotNull(dependency.toId);
    assertEquals(1, host.reportedSources.size());
    assertEquals(dependency.fromId, model().classes().get(0).attributes.get(0).id);
    assertEquals(dependency.toId, model().classes().get(1).attributes.get(1).id);
  }

  /** A class and its own attributes, and two attributes of one class, are no ends of one dependency. */
  @Test
  void notWithinOneClass() throws ClassDiagramParseException {
    openWithTool("");

    draw(onAttribute("age"), aboveAttributes("A"));
    assertTrue(editing.isDependencyToolArmed());
    draw(onAttribute("age"), onAttribute("name"));

    assertTrue(model().relations().isEmpty());
    assertTrue(editing.isDependencyToolArmed());
    assertTrue(host.reportedSources.isEmpty());
  }

  /** The tool doesn't start at a note. */
  @Test
  void notFromNote() throws ClassDiagramParseException {
    openWithTool("");

    draw(center(editing.scene().elementAreas.get(3).area), inClass("B"));

    assertTrue(host.reportedSources.isEmpty());
  }

  /** While the mouse is on an attribute, its line is framed. */
  @Test
  void attributeFramedWhileDrawing() throws ClassDiagramParseException {
    openWithTool("");
    control.pressed(inClass("C"));

    control.dragged(onAttribute("other"));

    assertNotNull(editing.connectionTargetArea());
    assertTrue(editing.connectionTargetArea().contains(onAttribute("other")));
  }

  /** Changes which are tracked mark the new dependency as added. */
  @Test
  void markedAddedWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    openWithTool("");

    draw(inClass("C"), onAttribute("other"));

    assertEquals(ChangeType_V001.ADDED, onlyDependency().change.type);
    assertEquals("yellow", onlyDependency().change.changeSet);
  }

  /** A class which a change set removed is neither start nor end. */
  @Test
  void notWithRemovedClass() throws ClassDiagramParseException {
    open(DIAGRAM.replace("class C (33333333) at(400, 300)", "class C (33333333) change(removed, yellow) at(400, 300)"),
      "");
    editing.toggleDependencyTool();

    draw(inClass("A"), inClass("C"));
    draw(inClass("C"), inClass("A"));

    assertTrue(host.reportedSources.isEmpty());
  }

  /** The button arms the tool, another tool takes it over, the next toggle puts it away. */
  @Test
  void toolToggled() throws ClassDiagramParseException {
    open("");

    editing.toggleDependencyTool();
    assertTrue(editing.isDependencyToolArmed());
    editing.toggleAnchorTool();
    assertFalse(editing.isDependencyToolArmed());
    editing.toggleDependencyTool();
    editing.toggleDependencyTool();
    assertFalse(editing.isToolArmed());
  }

  // ---- editing the line ----

  private void select(Point2D onLine) {
    editing.selectAt(onLine, TOLERANCE);
  }

  private Point2D middleOfLine() {
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    return new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
  }

  /** The end of a dependency can be hung onto an attribute of another class: it is its end from then on. */
  @Test
  void endHungOntoAttribute() throws ClassDiagramParseException {
    open("  dependency (55555555) 11111111 -> 33333333\n");
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(onAttribute("other"), 6);
    editing.endDrag();

    DependencyRelationModel_V001 dependency = onlyDependency();
    assertEquals("0b0b0b0b", dependency.toId);
    assertEquals("0b0b0b0b", dependency.route.end.classId);
    assertEquals("11111111", dependency.fromId);
  }

  /** An end can't be hung onto the class of the other end, nor onto its attributes. */
  @Test
  void endNotHungOntoClassOfOtherEnd() throws ClassDiagramParseException {
    open("  dependency (55555555) 11111111 -> 33333333\n");
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(onAttribute("age"), 6);
    editing.endDrag();

    assertEquals("33333333", onlyDependency().toId);
  }

  /** While changes are tracked an end stays where it hangs. */
  @Test
  void endStaysWhileTracking() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("  dependency (55555555) change(added, yellow) 11111111 -> 33333333\n");
    select(middleOfLine());
    Point2D end = editing.selectedHandles().get(1);

    editing.startDrag(end, TOLERANCE);
    editing.dragTo(onAttribute("other"), 6);
    editing.endDrag();

    assertEquals("33333333", onlyDependency().toId);
  }

  /** A waypoint can be put into a dependency whose start is an attribute. */
  @Test
  void waypointInDependencyFromAttribute() throws ClassDiagramParseException {
    open("  dependency (55555555) 0a0a0a0a -> 33333333\n");
    select(middleOfLine());
    Point2D start = editing.scene().relationLines.get(0).points().get(0);

    assertTrue(editing.insertWaypointAt(middleOfLine(), TOLERANCE));

    DependencyRelationModel_V001 dependency = onlyDependency();
    assertEquals(1, dependency.route.waypoints.size());
    assertEquals("0a0a0a0a", dependency.route.start.classId);
    SceneRelationLine line = editing.scene().relationLines.get(0);
    assertEquals(3, line.points().size());
    assertEquals(start.getX(), line.points().get(0).getX(), 1);
    assertEquals(start.getY(), line.points().get(0).getY(), 1);
  }

  /** The waypoints of a routed dependency are dragged like those of any relation. */
  @Test
  void waypointDragged() throws ClassDiagramParseException {
    open("  dependency (55555555) 0a0a0a0a -> 33333333\n    route 0a0a0a0a.right@8 (200, 200) 33333333.left@10\n");
    select(new Point2D.Double(200, 200));

    editing.startDrag(new Point2D.Double(200, 200), TOLERANCE);
    editing.dragTo(new Point2D.Double(250, 220));
    editing.endDrag();

    PositionModel_V001 waypoint = onlyDependency().route.waypoints.get(0);
    assertEquals(250, waypoint.x);
    assertEquals(220, waypoint.y);
  }

  /** A dependency leaves the picture, and the selection, when the compartment of its attribute is hidden. */
  @Test
  void hidingCompartmentDeselectsDependency() throws ClassDiagramParseException {
    open("  dependency (55555555) 0a0a0a0a -> 33333333\n");
    editing.selectAt(inClass("A"), TOLERANCE);
    ClassModel_V001 a = editing.selectedClass();
    select(middleOfLine());
    assertNotNull(editing.selectedRelation());

    editing.setAttributesHidden(a, true);

    assertNull(editing.selectedRelation());
    assertTrue(editing.scene().relationLines.isEmpty());
    assertEquals(1, model().relations().size());
  }

  /** Deleting the selected dependency removes only the dependency. */
  @Test
  void deleteDependency() throws ClassDiagramParseException {
    open("  dependency (55555555) 0a0a0a0a -> 33333333\n");
    select(middleOfLine());

    editing.deleteSelected();

    assertTrue(model().relations().isEmpty());
    assertEquals(3, model().classes().size());
  }
}
