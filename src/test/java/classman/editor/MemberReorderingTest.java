package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Selecting the row of an attribute, method or enum value, and changing the order of the rows. */
class MemberReorderingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String ATTRIBUTES = "classdiagram {\n  class A (11111111) at(10, 20) {\n"
    + "    -x\n    -y\n    -z\n  }\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
  }

  private Point2D rowPoint(String name) {
    SceneTextArea row = editing.scene().textAreas.stream()
      .filter(text -> text.member != null && name.equals(text.member.name)).findFirst().orElseThrow();
    return new Point2D.Double(row.area.getCenterX(), row.area.getCenterY());
  }

  private void press(Point2D at) {
    editing.selectAt(at);
    editing.startDrag(at);
  }

  private void click(Point2D at) {
    press(at);
    editing.endDrag();
  }

  /** Selects the class, then the row, the way the user does it with two clicks. */
  private void selectRow(String name) {
    click(rowPoint(name));
    click(rowPoint(name));
  }

  private List<String> names(List<? extends AbstractMemberModel_V001> members) {
    return members.stream().map(member -> member.name).toList();
  }

  private ClassModel_V001 written() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  /** The first press on a row selects the class only, the next one the row. */
  @Test
  void rowIsSelectedByTheSecondPress() throws ClassDiagramParseException {
    open(ATTRIBUTES);

    click(rowPoint("y"));
    assertNotNull(editing.selectedClass());
    assertNull(editing.selectedMember());

    click(rowPoint("y"));
    assertEquals("y", editing.selectedMember().name);
    assertNotNull(editing.selectionMarks().memberRow);
    assertTrue(host.reportedSources.isEmpty());
  }

  /** The class is selected too, and selecting something else takes the row away. */
  @Test
  void rowGoesWithTheSelection() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");

    click(rowPoint("z"));
    assertEquals("z", editing.selectedMember().name);

    editing.selectAt(new Point2D.Double(400, 400));
    assertNull(editing.selectedMember());
    assertNull(editing.selectedClass());
  }

  /** Esc takes the row away first and the class with the next one. */
  @Test
  void rowEndsBeforeTheClass() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");

    assertTrue(editing.deselectMember());
    assertNotNull(editing.selectedClass());
    assertFalse(editing.deselectMember());
  }

  /** Alt+Up and Alt+Down move the row one place, which is one operation each. */
  @Test
  void rowMovedByKey() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");

    editing.moveSelectedMember(-1);
    assertEquals(List.of("y", "x", "z"), names(written().attributes));
    assertEquals(1, host.reportedSources.size());
    assertEquals("y", editing.selectedMember().name);

    editing.moveSelectedMember(1);
    editing.moveSelectedMember(1);
    assertEquals(List.of("x", "z", "y"), names(written().attributes));
    assertEquals(3, host.reportedSources.size());
  }

  /** Nothing happens at the first and the last place, and no operation is made. */
  @Test
  void rowStopsAtTheEnds() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("x");

    editing.moveSelectedMember(-1);

    assertEquals(List.of("x", "y", "z"), names(written().attributes));
    assertTrue(host.reportedSources.isEmpty());
  }

  /** The row follows the mouse while it is dragged, and the operation is reported when the mouse is released. */
  @Test
  void rowDragged() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("x");
    Point2D from = rowPoint("x");
    double rowHeight = rowPoint("y").getY() - from.getY();

    press(from);
    editing.dragTo(new Point2D.Double(from.getX(), from.getY() + 2.5 * rowHeight));

    assertEquals(List.of("y", "z", "x"), names(editing.selectedClass().attributes));
    assertEquals("x", editing.selectedMember().name);
    assertTrue(host.reportedSources.isEmpty());

    editing.endDrag();

    assertEquals(List.of("y", "z", "x"), names(written().attributes));
    assertEquals(1, host.reportedSources.size());
    assertEquals("x", editing.selectedMember().name);
  }

  /** Esc during the drag puts the row back and reports nothing. */
  @Test
  void dragCancelled() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("x");
    Point2D from = rowPoint("x");

    press(from);
    editing.dragTo(new Point2D.Double(from.getX(), from.getY() + 100));
    assertTrue(editing.cancelDrag());

    assertEquals(List.of("x", "y", "z"), names(editing.selectedClass().attributes));
    assertTrue(host.reportedSources.isEmpty());
  }

  /** A row which is dragged back to where it was is no operation. */
  @Test
  void dragBackIsNoOperation() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");
    Point2D from = rowPoint("y");

    press(from);
    editing.dragTo(new Point2D.Double(from.getX(), from.getY() + 100));
    editing.dragTo(from);
    editing.endDrag();

    assertTrue(host.reportedSources.isEmpty());
  }

  /** A drag on a row which isn't selected yet moves the class, and the selection of the row ends with the move. */
  @Test
  void unselectedRowMovesTheClass() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    click(rowPoint("y"));
    double left = editing.selectedArea().getX();
    Point2D from = rowPoint("y");

    press(from);
    assertEquals("y", editing.selectedMember().name);
    editing.dragTo(new Point2D.Double(from.getX() + 50, from.getY()));
    editing.endDrag();

    assertEquals(left + 50, editing.selectedArea().getX(), 0.001);
    assertNull(editing.selectedMember());
    assertEquals(List.of("x", "y", "z"), names(written().attributes));
  }

  /** A click on a selected row only keeps it selected. */
  @Test
  void clickOnSelectedRowChangesNothing() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");

    click(rowPoint("y"));

    assertEquals("y", editing.selectedMember().name);
    assertTrue(host.reportedSources.isEmpty());
  }

  /** The methods are reordered among the methods, the attributes stay where they are. */
  @Test
  void methodsAreAnotherCompartment() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x\n    -y\n    +save()\n    +load()\n  }\n}");
    selectRow("load");

    editing.moveSelectedMember(-1);
    editing.moveSelectedMember(-1);

    assertEquals(List.of("x", "y"), names(written().attributes));
    assertEquals(List.of("load", "save"), names(written().methods));
  }

  /** The values of an enum are reordered like attributes. */
  @Test
  void enumValues() throws ClassDiagramParseException {
    open("classdiagram {\n  enum Color (11111111) at(10, 20) {\n    RED\n    GREEN\n    BLUE\n  }\n}");
    selectRow("RED");

    editing.moveSelectedMember(1);

    assertEquals(List.of("GREEN", "RED", "BLUE"), names(written().attributes));
  }

  /** Hidden attributes have no row. The moved row stands directly in front of the shown one it is put before. */
  @Test
  void hiddenMembersAreNoPlaces() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -a\n    -h (hidden)\n    -b\n  }\n}");
    selectRow("b");

    editing.moveSelectedMember(-1);

    assertEquals(List.of("b", "a", "h"), names(written().attributes));
    assertTrue(written().attributes.get(2).hidden);
  }

  /** The ID of a member stays with it, which is how notes and dependencies keep their targets. */
  @Test
  void idsMoveWithTheirMembers() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x (aaaaaaaa)\n    -y (bbbbbbbb)\n  }\n}");
    selectRow("y");

    editing.moveSelectedMember(-1);

    assertEquals("bbbbbbbb", written().attributes.get(0).id);
    assertEquals("aaaaaaaa", written().attributes.get(1).id);
  }

  /** The order is no change: nothing is marked while changes are tracked, and the marks which are there stay with
   * their members. */
  @Test
  void orderIsNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x change(added, blue)\n    -y\n  }\n}");
    selectRow("y");

    editing.moveSelectedMember(-1);

    assertEquals(List.of("y", "x"), names(written().attributes));
    assertEquals("blue", written().attributes.get(1).change.changeSet);
    assertNull(written().attributes.get(0).change);
    assertFalse(editing.source().contains("yellow"), editing.source());
  }

  /** The rows of a class which a change set has removed can't be selected. */
  @Test
  void removedClassHasNoSelectableRows() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) change(removed, blue) at(10, 20) {\n    -x\n    -y\n  }\n}");
    click(rowPoint("y"));

    click(rowPoint("y"));

    assertNull(editing.selectedMember());
  }

  /** Delete takes away the selected row and not the class. */
  @Test
  void deleteTakesTheRow() throws ClassDiagramParseException {
    open(ATTRIBUTES);
    selectRow("y");

    editing.deleteSelected();

    assertEquals(List.of("x", "z"), names(written().attributes));
    assertNull(editing.selectedMember());
    assertNotNull(editing.selectedClass());
  }

  /** While changes are tracked the row of a deleted member stays, marked as removed, like any deleted member. */
  @Test
  void deleteIsMarkedWhileTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(ATTRIBUTES);
    selectRow("y");

    editing.deleteSelected();

    assertEquals(List.of("x", "y", "z"), names(written().attributes));
    assertEquals("yellow", written().attributes.get(1).change.changeSet);
    assertNull(editing.selectedMember());
  }
}
