package headless.plantuml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.plantuml.PlantUmlImageEditArea;
import specman.view.AbstractSchrittView;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.ACCEPT_CHANGES_BUTTON;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;

/** The source of a diagram is tracked like a text: A change in change mode keeps the source from before as backup,
 * which marks the diagram as changed and allows to reject the change. */
class PlantUmlChangeModeTest extends AbstractPlantUmlTest {

  private static final ChangeSet BLUE = ChangeSet.ALL.get("blue");
  private static final ChangeSet PINK = ChangeSet.ALL.get("pink");

  /** Scenario: Without change mode, a source change is an ordinary edit. No backup is kept and the area stays uncolored. */
  @Test
  void testSourceChangeOutsideChangeModeIsNotMarked() {
    PlantUmlImageEditArea diagram = existingDiagram();

    diagram.changeSource(THREE_CLASSES);

    assertUnchanged(diagram);
  }

  /** Scenario: The first source change in change mode keeps the source from before as backup, together with the
   * change set, and colors the area with the change set's color. The area itself is not marked as added or deleted,
   * because the change belongs to its content, like the markups of a text. */
  @Test
  void testFirstSourceChangeColorsTheAreaAndKeepsTheSourceFromBefore() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);

    diagram.changeSource(THREE_CLASSES);

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertNotNull(diagram.getBackup(), "backup");
    assertEquals(TWO_CLASSES, diagram.getBackup().source());
    assertEquals(BLUE, diagram.getBackup().changeSet());
    assertEquals(BLUE.panelColor(), diagram.getBackground());
    assertTrue(diagram.getChangeInfo().isUntracked(), "the area itself is neither added nor deleted");
  }

  /** Scenario: A second change of an already changed diagram keeps the backup of the first change, so rejecting
   * still leads back to the source from before all changes. */
  @Test
  void testFurtherChangesKeepTheFirstBackup() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);

    diagram.changeSource(THREE_CLASSES.replace("Universe", "Cosmos"));

    assertEquals(TWO_CLASSES, diagram.getBackup().source());
  }

  /** Scenario: If the user changes the source back to what it was before, nothing is left to review. The backup is
   * dropped and the area looks as if it had never been touched. */
  @Test
  void testChangingTheSourceBackToTheBackupTakesTheMarkAway() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);

    diagram.changeSource(TWO_CLASSES);

    assertUnchanged(diagram);
  }

  /** Scenario: Undoing the first change takes source, backup and color away in a single step. Redo brings all of
   * them back. */
  @Test
  void testUndoTakesBackupAndColorAwayInOneStepAndRedoBringsThemBack() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);

    specman.undo();

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertUnchanged(diagram);

    specman.redo();

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertEquals(TWO_CLASSES, diagram.getBackup().source());
    assertEquals(BLUE.panelColor(), diagram.getBackground());
  }

  /** Scenario: Rejecting the changes restores the source from the backup, and the area is uncolored again. */
  @Test
  void testRejectingBringsTheSourceFromBeforeBack() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);

    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertUnchanged(diagram);
  }

  /** Scenario: Accepting the changes keeps the changed source, drops the backup and uncolors the area. */
  @Test
  void testAcceptingKeepsTheChangedSource() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertUnchanged(diagram);
  }

  /** Scenario: A diagram changed by blue is changed again with pink being the current change set. The change stays
   * with blue: Rejecting for pink leaves the diagram alone, rejecting for blue restores the source from before
   * blue's change. */
  @Test
  void testAnotherChangeSetChangingTheSourceDoesNotTakeOverTheChange() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);
    specman.selectChangeSet(PINK);

    diagram.changeSource(THREE_CLASSES.replace("Universe", "Cosmos"));
    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(BLUE, diagram.getBackup().changeSet(), "the change stays with the first change set");
    assertEquals(THREE_CLASSES.replace("Universe", "Cosmos"), diagram.getSource());

    specman.selectChangeSet(BLUE);
    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertUnchanged(diagram);
  }

  /** Scenario: Deleting a changed diagram in change mode shows the source from before the change as deleted, like
   * the previously existing passages of a text. The change itself is gone, and rejecting the deletion leaves a
   * diagram with the original source and no marks. */
  @Test
  void testDeletingAChangedDiagramMarksTheSourceFromBeforeAsDeleted() {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);
    specman.focus(diagram);

    specman.press(KeyEvent.VK_DELETE);

    assertTrue(diagram.getChangeInfo().isDeleted(), "diagram must be marked as deleted");
    assertEquals(TWO_CLASSES, diagram.getSource());
    assertNull(diagram.getBackup());
    assertEquals(BLUE.panelColor(), diagram.getBackground());

    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(TWO_CLASSES, diagram.getSource());
    assertUnchanged(diagram);
  }

  /** Scenario: The file holds the changed source followed by the backup with its change set. After loading,
   * the diagram is colored again and rejecting the change still restores the source from before. */
  @Test
  void testChangeSurvivesSavingAndLoadingAndCanBeRejectedAfterwards(@TempDir Path directory) throws IOException {
    PlantUmlImageEditArea diagram = existingDiagram();
    startChangeMode(BLUE);
    diagram.changeSource(THREE_CLASSES);
    File file = directory.resolve("plantuml.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    assertTrue(saved.contains("`" + THREE_CLASSES + "`,"), "changed source");
    assertTrue(saved.contains("backup=(blue, `" + TWO_CLASSES + "`)"), "backup behind the source");

    specman.diagrammLaden(file);

    PlantUmlImageEditArea reloaded = reloadedDiagram();
    assertEquals(THREE_CLASSES, reloaded.getSource());
    assertEquals(TWO_CLASSES, reloaded.getBackup().source());
    assertEquals(BLUE, reloaded.getBackup().changeSet());
    assertEquals(BLUE.panelColor(), reloaded.getBackground());
    specman.click(REJECT_CHANGES_BUTTON);
    assertEquals(TWO_CLASSES, reloaded.getSource());
  }

  /** Scenario: A diagram nobody changed is saved as the compact one-line image block, without a backup. */
  @Test
  void testUnchangedDiagramIsSavedWithoutBackup(@TempDir Path directory) throws IOException {
    existingDiagram();
    File file = directory.resolve("plantuml.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);

    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    assertTrue(saved.contains("{ `" + TWO_CLASSES + "` }"), "single line image block");
    assertTrue(!saved.contains(TWO_CLASSES + "`,"), "no source followed by a backup");
  }

  private static PlantUmlImageEditArea existingDiagram() {
    PlantUmlImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());
    diagram.commitSource(TWO_CLASSES);
    return diagram;
  }

  private static void startChangeMode(ChangeSet changeSet) {
    specman.toggleChangeMode();
    specman.selectChangeSet(changeSet);
  }

  private static void assertUnchanged(PlantUmlImageEditArea diagram) {
    assertNull(diagram.getBackup(), "backup");
    assertEquals(BACKGROUND_COLOR_STANDARD, diagram.getBackground());
  }

  private static PlantUmlImageEditArea reloadedDiagram() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    EditContainer container = step.getFirstEditArea().asTextArea().getParent();
    return (PlantUmlImageEditArea) container.getEditAreaAt(1);
  }
}
