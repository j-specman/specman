package headless.image.classman;

import classman.editor.DiagramEditing;
import classman.editor.EditorTexts;
import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.editarea.image.classman.ClassmanEditorHost;
import specman.editarea.image.classman.ClassmanImageEditArea;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The diagram editor reports each operation to the edit area, so Specman's undo history holds them. */
class ClassmanEditorSessionTest extends AbstractClassmanTest {

  /** Scenario: Undo and redo of the editor take the operation back and forth in the area, and the editor shows what
   * the area has. */
  @Test
  void undoAndRedoInTheEditor() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    editing.addClass(300, 20);
    String withNewClass = diagram.getSource();
    assertTrue(withNewClass.contains("NewClass"));

    editing.undo();

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertEquals(TWO_CLASSES, editing.source());
    assertTrue(editing.canRedo());

    editing.redo();

    assertEquals(withNewClass, diagram.getSource());
    assertEquals(withNewClass, editing.source());
  }

  /** Scenario: Each operation of the editor is one step in Specman's history, which works after the editor has been
   * closed. */
  @Test
  void undoAfterTheEditorClosed() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    editing.addClass(300, 20);
    editing.addClass(500, 20);

    specman.undo();

    assertEquals(1, diagram.getSource().split("NewClass", -1).length - 1);
    specman.undo();
    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: The editor doesn't undo what was done before it was opened. */
  @Test
  void undoStopsAtSessionStart() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    diagram.changeSource(THREE_CLASSES);
    DiagramEditing editing = editorOn(diagram);
    editing.addClass(300, 20);

    editing.undo();
    editing.undo();

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertFalse(editing.canUndo());
  }

  /** Scenario: A new operation after an undo ends the redo possibility, as everywhere in Specman. */
  @Test
  void newOperationEndsRedo() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    editing.addClass(300, 20);
    editing.undo();

    editing.addClass(500, 20);

    assertFalse(editing.canRedo());
  }

  /** Scenario: In change mode, a class which the editor creates is marked as added by the active change set. */
  @Test
  void classAddedInChangeMode() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    specman.toggleChangeMode();
    specman.selectChangeSet(ChangeSet.ALL.get("blue"));

    editorOn(diagram).addClass(300, 20);

    assertTrue(diagram.getSource().contains("change(added, blue)"), diagram.getSource());
  }

  /** Scenario: A move is one step of Specman's history, even if it fixes the places of the stacked classes. */
  @Test
  void moveIsOneUndoStep() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    Rectangle2D hello = editing.scene().elementAreas.get(0).area;
    Point2D grab = new Point2D.Double(hello.getCenterX(), hello.getCenterY());
    editing.selectAt(grab);

    editing.startDrag(grab);
    editing.dragTo(new Point2D.Double(grab.getX() + 200, grab.getY()));
    editing.endDrag();

    assertTrue(diagram.getSource().contains("at("), diagram.getSource());
    specman.undo();
    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: Deleting a class is one step of Specman's history. */
  @Test
  void deletionIsOneUndoStep() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    Rectangle2D hello = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(hello.getCenterX(), hello.getCenterY()));

    editing.deleteSelected();

    assertFalse(diagram.getSource().contains("Hello"), diagram.getSource());
    specman.undo();
    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: Renaming a class is one step of Specman's history. */
  @Test
  void renamingIsOneUndoStep() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    DiagramEditing editing = editorOn(diagram);
    Rectangle2D hello = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(hello.getCenterX(), hello.getY() + 3));

    editing.rename(editing.selectedClass(), "Greeting");

    assertTrue(diagram.getSource().contains("Greeting"), diagram.getSource());
    specman.undo();
    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  private static DiagramEditing editorOn(ClassmanImageEditArea diagram) throws ClassDiagramParseException {
    return new DiagramEditing(diagram.getSource(), new ClassmanEditorHost(diagram), new EditorTexts(Locale.ENGLISH));
  }
}
