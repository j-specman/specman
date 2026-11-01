package headless.image.plantuml;

import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.image.plantuml.PlantUmlContextMenu;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;

import javax.swing.JMenuItem;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;

/** The context menu of a PlantUML diagram. Its item to edit opens the editor next to the diagram, which needs a
 * diagram on screen and is therefore not covered here. */
class PlantUmlContextMenuTest extends AbstractPlantUmlTest {

  /** Scenario: A right click on a diagram offers to edit, import into and delete it. */
  @Test
  void testMenuOffersEditImportAndDelete() {
    PlantUmlImageEditArea diagram = existingDiagram(containerOfNewStep());

    JMenuItem edit = specman.contextMenuItem(diagram, PlantUmlContextMenu.EDIT_ITEM);
    JMenuItem importSource = specman.contextMenuItem(diagram, PlantUmlContextMenu.IMPORT_ITEM);
    JMenuItem delete = specman.contextMenuItem(diagram, PlantUmlContextMenu.DELETE_ITEM);

    assertTrue(edit.isEnabled() && edit.isVisible(), "edit must be available");
    assertTrue(importSource.isEnabled() && importSource.isVisible(), "import must be available");
    assertTrue(delete.isEnabled() && delete.isVisible(), "delete must be available");
  }

  /** Scenario: Deleting from the menu takes the diagram out. Undo brings it back. */
  @Test
  void testDeleteRemovesTheDiagramAndCanBeUndone() {
    EditContainer container = containerOfNewStep();
    PlantUmlImageEditArea diagram = existingDiagram(container);

    specman.clickContextMenuItem(diagram, PlantUmlContextMenu.DELETE_ITEM);

    assertFalse(hasDiagram(container), "the diagram must be gone");

    specman.undo();

    assertTrue(hasDiagram(container), "undo must bring the diagram back");
  }

  /** Scenario: In change mode, deleting a diagram which existed before only marks it as deleted. Rejecting the
   * changes takes the deletion back. */
  @Test
  void testDeleteInChangeModeMarksTheDiagramAsDeleted() {
    EditContainer container = containerOfNewStep();
    PlantUmlImageEditArea diagram = existingDiagram(container);
    specman.toggleChangeMode();
    specman.selectChangeSet(ChangeSet.ALL.get("blue"));

    specman.clickContextMenuItem(diagram, PlantUmlContextMenu.DELETE_ITEM);

    assertTrue(hasDiagram(container), "the diagram must stay as deleted");
    assertTrue(diagram.getChangeInfo().isDeleted(), "the diagram must be marked as deleted");

    specman.click(REJECT_CHANGES_BUTTON);

    assertTrue(diagram.getChangeInfo().isUntracked(), "the deletion must be taken back");
  }

  private static PlantUmlImageEditArea existingDiagram(EditContainer container) {
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(container);
    diagram.commitSource(TWO_CLASSES);
    return diagram;
  }
}
