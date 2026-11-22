package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneRelationLine;
import classman.render.SceneTextArea;
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

class AnchorToAttributeAndRelationTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20) {\n    name\n"
    + "    age (0a0a0a0a)\n  }\n  class B (22222222) at(400, 20)\n"
    + "  association (44444444) 11111111 -- 22222222\n  note (33333333) at(10, 300) \"remember\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
    editing.toggleAnchorTool();
  }

  private Point2D center(Rectangle2D area) {
    return new Point2D.Double(area.getCenterX(), area.getCenterY());
  }

  private Rectangle2D lineOfAttribute(String name) {
    return editing.scene().textAreas.stream()
      .filter(text -> text.kind == TextKind.ATTRIBUTE_LINE && text.attribute != null && name.equals(text.attribute.name))
      .map(text -> text.area).findFirst().orElseThrow();
  }

  private Point2D noteCenter() {
    return center(editing.scene().elementAreas.stream().filter(area -> area.element instanceof NoteModel_V001)
      .findFirst().orElseThrow().area);
  }

  private Point2D onRelation() {
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    return new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
  }

  private void draw(Point2D from, Point2D to) {
    control.pressed(from);
    control.dragged(to);
    control.released(to);
  }

  private ClassModel_V001 classA() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  private String attachedTo() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).notes().get(0).attachedToId;
  }

  /** An attribute which has an ID is taken as it is. */
  @Test
  void noteAnchoredToAttributeWithId() throws ClassDiagramParseException {
    open(DIAGRAM);

    draw(noteCenter(), center(lineOfAttribute("age")));

    assertEquals("0a0a0a0a", attachedTo());
    assertFalse(editing.isToolArmed());
    assertEquals(1, host.reportedSources.size());
    assertNotNull(editing.selectedNote());
  }

  /** An attribute which has no ID gets one, in the same operation which attaches the note, and the dashed line leads to it. */
  @Test
  void attributeWithoutIdGetsOne() throws ClassDiagramParseException {
    open(DIAGRAM);

    draw(noteCenter(), center(lineOfAttribute("name")));

    AttributeModel_V001 name = classA().attributes.get(0);
    assertNotNull(name.id);
    assertEquals(name.id, attachedTo());
    assertEquals(1, host.reportedSources.size());
    assertEquals(1, editing.scene().anchorLines.size());
  }

  /** On the lines of the attributes the attribute wins, around them the class does. */
  @Test
  void attributeWinsOverItsClass() throws ClassDiagramParseException {
    open(DIAGRAM);
    SceneTextArea className = editing.scene().textAreas.stream()
      .filter(text -> text.kind == TextKind.CLASS_NAME).findFirst().orElseThrow();

    draw(noteCenter(), center(className.area));

    assertEquals("11111111", attachedTo());
  }

  @Test
  void noteAnchoredToRelation() throws ClassDiagramParseException {
    open(DIAGRAM);

    draw(noteCenter(), onRelation());

    assertEquals("44444444", attachedTo());
    assertEquals(1, editing.scene().anchorLines.size());
    assertEquals(1, host.reportedSources.size());
  }

  /** A relation which has no ID gets one, because the note refers to it by ID. */
  @Test
  void relationWithoutIdGetsOne() throws ClassDiagramParseException {
    open(DIAGRAM.replace(" (44444444)", ""));

    draw(noteCenter(), onRelation());

    String relationId = new ClassDiagramParser_V001().parse(editing.source()).relations().get(0).id;
    assertNotNull(relationId);
    assertEquals(relationId, attachedTo());
  }

  /** While the mouse is on an attribute its line is framed, on a relation the line of the relation is drawn thick. */
  @Test
  void targetIsShownWhileDragging() throws ClassDiagramParseException {
    open(DIAGRAM);
    double plainWidth = editing.scene().relationLines.get(0).line.strokeWidth;

    control.pressed(noteCenter());
    control.dragged(center(lineOfAttribute("age")));
    assertEquals(lineOfAttribute("age"), editing.connectionTargetArea());
    assertEquals(plainWidth, editing.scene().relationLines.get(0).line.strokeWidth);

    control.dragged(onRelation());
    assertNull(editing.connectionTargetArea());
    SceneRelationLine thick = editing.scene().relationLines.get(0);
    assertTrue(thick.line.strokeWidth > plainWidth);

    control.dragged(new Point2D.Double(300, 600));
    assertEquals(plainWidth, editing.scene().relationLines.get(0).line.strokeWidth);
    control.released(new Point2D.Double(300, 600));
    assertNull(attachedTo());
  }

  /** The note stays on its attribute: anchoring it to the same one again is no operation. */
  @Test
  void sameAttributeIsNoOperation() throws ClassDiagramParseException {
    open(DIAGRAM.replace("\"remember\"", "\"remember\" attach(0a0a0a0a)"));

    draw(noteCenter(), center(lineOfAttribute("age")));

    assertTrue(host.reportedSources.isEmpty());
    assertFalse(editing.canUndo());
  }

  /** A note which is attached to an attribute is moved to a relation, and the waypoints of the old line go. */
  @Test
  void anchorMovedFromAttributeToRelation() throws ClassDiagramParseException {
    open(DIAGRAM.replace("\"remember\"", "\"remember\" attach(0a0a0a0a) route (200, 250)"));

    draw(noteCenter(), onRelation());

    assertEquals("44444444", attachedTo());
    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).notes().get(0).attachWaypoints.isEmpty());
  }

  /** An attribute which a change set removed is no target any more: the point is on its class then. */
  @Test
  void removedAttributeIsNoTarget() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(DIAGRAM.replace("age (0a0a0a0a)", "age (0a0a0a0a) change(removed, yellow)")
      .replace("note (33333333)", "note (33333333) change(added, yellow)"));

    draw(noteCenter(), center(lineOfAttribute("age")));

    assertEquals("11111111", attachedTo());
  }
}
