package classman.editor;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.Side_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorEndSlideTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String DIAGRAM = "classdiagram {\n  class A (11111111) at(10, 20) {\n    name\n"
    + "    age (0a0a0a0a)\n  }\n  class B (22222222) at(400, 20)\n"
    + "  association (44444444) 11111111 -- 22222222\n  note (33333333) at(150, 300) \"remember\"\n"
    + "  anchor 33333333 -- %s\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String attachedTo) throws ClassDiagramParseException {
    editing = new DiagramEditing(String.format(DIAGRAM, attachedTo), host);
    control = new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels);
    List<Point2D> points = editing.scene().anchorLines.get(0).points();
    Point2D middle = new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2);
    control.pressed(middle);
    control.released(middle);
  }

  private Point2D endHandle() {
    List<Point2D> handles = editing.selectedHandles();
    return handles.get(handles.size() - 1);
  }

  private void dragEndTo(Point2D to) {
    control.pressed(endHandle());
    control.dragged(to);
    control.released(to);
  }

  private AnchorLineModel_V001 anchorLine() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).anchorLines().get(0);
  }

  /** The end of an anchor on an attribute has a handle, like the one on a class. */
  @Test
  void attributeAnchorHasEndHandle() throws ClassDiagramParseException {
    open("0a0a0a0a");

    assertEquals(2, editing.selectedHandles().size());
  }

  /** The end slides along the border of the attribute text and keeps the attribute: a drag far to the left puts it on
   * the left side. One operation. */
  @Test
  void endSlidesAlongAttributeBorder() throws ClassDiagramParseException {
    open("0a0a0a0a");
    Point2D before = endHandle();

    dragEndTo(new Point2D.Double(-300, before.getY()));

    assertEquals("0a0a0a0a", anchorLine().targetId);
    assertEquals("0a0a0a0a", anchorLine().end.classId);
    assertEquals(Side_V001.LEFT, anchorLine().end.side);
    assertEquals(1, host.reportedSources.size());
    assertTrue(endHandle().getX() < before.getX() || before.getX() == endHandle().getX());
  }

  /** The end slides along the line of a relation and is kept as a percentage of its length. */
  @Test
  void endSlidesAlongRelation() throws ClassDiagramParseException {
    open("44444444");
    List<Point2D> relation = editing.scene().relationLines.get(0).points();
    Point2D start = relation.get(0);
    Point2D end = relation.get(relation.size() - 1);
    Point2D quarter = new Point2D.Double(start.getX() + (end.getX() - start.getX()) / 4,
      start.getY() + (end.getY() - start.getY()) / 4);

    dragEndTo(new Point2D.Double(quarter.getX(), quarter.getY() + 80));

    assertEquals("44444444", anchorLine().targetId);
    assertTrue(anchorLine().end.isOnLine());
    assertEquals(25, anchorLine().end.offset, 1);
    assertEquals(quarter.getX(), endHandle().getX(), 3);
    assertEquals(quarter.getY(), endHandle().getY(), 3);
    assertEquals(1, host.reportedSources.size());
  }

  /** The end can't leave the relation: dragged beyond the line it stays at its end. */
  @Test
  void endStopsAtEndOfRelation() throws ClassDiagramParseException {
    open("44444444");
    List<Point2D> relation = editing.scene().relationLines.get(0).points();
    Point2D end = relation.get(relation.size() - 1);

    dragEndTo(new Point2D.Double(end.getX() + 500, end.getY()));

    assertEquals(100, anchorLine().end.offset);
  }

  /** Esc puts the end back where it was. */
  @Test
  void escPutsEndBack() throws ClassDiagramParseException {
    open("44444444");
    Point2D handle = endHandle();

    control.pressed(handle);
    control.dragged(new Point2D.Double(handle.getX() + 60, handle.getY()));
    assertTrue(editing.cancelDrag());

    assertNull(anchorLine().end);
    assertFalse(editing.canUndo());
  }
}
