package classman.editor;

import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupDeletionTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String DIAGRAM = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n"
    + "  class Page (11111111) at(300, 20)\n  class Cover (22222222) at(10, 400)\n"
    + "  note (33333333) at(300, 400) \"n\"\n"
    + "  association 04de91aa -> 11111111\n  anchor 33333333 -- 04de91aa\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();

  /** All selected boxes go with what hangs on them, as one operation, and nothing is selected afterwards. */
  @Test
  void groupIsDeletedInOneOperation() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAllBoxes();

    editing.deleteSelected();

    assertEquals(1, host.reportedSources.size());
    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).elements.isEmpty());
    assertFalse(editing.hasSelection());
  }

  /** A note whose class goes with the group is gone with it, whether it is selected or not, without a problem. */
  @Test
  void noteGoesWithItsClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.selectAllBoxes();

    editing.deleteSelected();

    assertTrue(new ClassDiagramParser_V001().parse(editing.source()).notes().isEmpty());
  }

  /** While changes are tracked, each box is marked as removed, or removed at once if the change set has added it. */
  @Test
  void trackedGroupIsMarkedRemoved() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(DIAGRAM.replace("class Page (11111111)",
      "class Page (11111111) change(added, yellow)"), host);
    editing.selectAllBoxes();

    editing.deleteSelected();

    List<ClassModel_V001> classes = new ClassDiagramParser_V001().parse(editing.source()).classes();
    assertEquals(List.of("Book", "Cover"), classes.stream().map(box -> box.name).toList());
    assertTrue(classes.stream().allMatch(box -> box.change.type == ChangeType_V001.REMOVED
      && box.change.changeSet.equals("yellow")));
    assertEquals(1, host.reportedSources.size());
    assertFalse(editing.hasSelection());
  }

  /** A single selected box is deleted as before. */
  @Test
  void singleBoxStillWorks() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(DIAGRAM, host);
    editing.pressWithCtrl(centerOfCover(editing), 4);

    editing.deleteSelected();

    assertEquals(List.of("Book", "Page"), new ClassDiagramParser_V001().parse(editing.source()).classes().stream()
      .map(box -> box.name).toList());
  }

  private static java.awt.geom.Point2D centerOfCover(DiagramEditing editing) {
    java.awt.geom.Rectangle2D area = editing.scene().elementAreas.stream()
      .filter(candidate -> candidate.element instanceof ClassModel_V001 box && box.name.equals("Cover"))
      .findFirst().orElseThrow().area;
    return new java.awt.geom.Point2D.Double(area.getCenterX(), area.getCenterY());
  }
}
