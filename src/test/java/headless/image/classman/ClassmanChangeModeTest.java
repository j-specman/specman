package headless.image.classman;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;
import specman.view.AbstractSchrittView;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.ACCEPT_CHANGES_BUTTON;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;

/** The change marks in the source of a diagram, change(...) and was(...), are resolved when the changes of a change
 * set are accepted or rejected, and renamed when change sets are merged. Nothing is tracked about the source as a
 * whole: Who changes the source in change mode writes the marks. */
class ClassmanChangeModeTest extends AbstractClassmanTest {

  private static final ChangeSet BLUE = ChangeSet.ALL.get("blue");
  private static final ChangeSet PINK = ChangeSet.ALL.get("pink");

  private static final String MARKED = """
      classdiagram {
          class A was(blue, Alpha) (aaaaaaaa)
          class B (bbbbbbbb) change(added, blue)
          class C (cccccccc) change(removed, blue)
          class D (dddddddd) change(added, pink)
      }
      """;

  /** Scenario: A source change is an ordinary edit, with or without change mode. It is not marked, the area stays
   * uncolored and shows the new diagram. */
  @Test
  void testSourceChangeIsNotTracked() {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    int heightBefore = diagram.getFullSizeImage().getHeight();
    startChangeMode(BLUE);

    diagram.changeSource(THREE_CLASSES);

    assertEquals(THREE_CLASSES, diagram.getSource());
    assertEquals(BACKGROUND_COLOR_STANDARD, diagram.getBackground());
    assertTrue(diagram.getChangeInfo().isUntracked(), "the area itself is neither added nor deleted");
    assertTrue(diagram.getFullSizeImage().getHeight() > heightBefore, "the third class makes the diagram higher");
  }

  /** Scenario: Accepting resolves the marks of the current change set: added things stay, removed things go, changed
   * texts keep their new text. Marks of other change sets stay. */
  @Test
  void testAcceptingResolvesTheMarksOfTheCurrentChangeSet() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(BLUE);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(canonical("""
        classdiagram {
            class A (aaaaaaaa)
            class B (bbbbbbbb)
            class D (dddddddd) change(added, pink)
        }
        """), diagram.getSource());
  }

  /** Scenario: Rejecting resolves the marks of the current change set the other way: added things go, removed things
   * stay, changed texts get their old text back. */
  @Test
  void testRejectingBringsTheStateFromBeforeBack() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(BLUE);

    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(canonical("""
        classdiagram {
            class Alpha (aaaaaaaa)
            class C (cccccccc)
            class D (dddddddd) change(added, pink)
        }
        """), diagram.getSource());
  }

  /** Scenario: A diagram without marks of the current change set keeps its source as it is, including its formatting. */
  @Test
  void testAcceptingWithoutMarksKeepsTheSource() {
    ClassmanImageEditArea diagram = existingDiagram(containerOfNewStep());
    startChangeMode(BLUE);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(TWO_CLASSES, diagram.getSource());
  }

  /** Scenario: Accepting is a single undo step. Undo brings the marks back, redo resolves them again. */
  @Test
  void testUndoBringsTheMarksBack() {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(BLUE);
    specman.click(ACCEPT_CHANGES_BUTTON);
    String accepted = diagram.getSource();

    specman.undo();

    assertEquals(MARKED, diagram.getSource());

    specman.redo();

    assertEquals(accepted, diagram.getSource());
  }

  /** Scenario: Merging a change set into the current one gives its marks the name of the current change set. */
  @Test
  void testMergingRenamesTheMarks() throws ClassDiagramParseException {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    specman.toggleChangeMode();
    specman.selectChangeSet(PINK);

    specman.findComponent(MergeChangeSetsADBLOpButton.class).mergeChangeSets(BLUE);

    assertEquals(canonical("""
        classdiagram {
            class A was(pink, Alpha) (aaaaaaaa)
            class B (bbbbbbbb) change(added, pink)
            class C (cccccccc) change(removed, pink)
            class D (dddddddd) change(added, pink)
        }
        """), diagram.getSource());
  }

  /** Scenario: The marks are part of the source in the file, which has no backup for a diagram. After loading,
   * accepting still resolves them. */
  @Test
  void testMarksSurviveSavingAndLoading(@TempDir Path directory) throws IOException {
    diagramWithSource(containerOfNewStep(), MARKED);
    File file = directory.resolve("classman.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    assertTrue(saved.contains("change(added, blue)") && saved.contains("was(blue, Alpha)"), "marks in the source");
    assertFalse(saved.contains("backup=(blue"), "no backup");

    specman.diagrammLaden(file);

    ClassmanImageEditArea reloaded = reloadedDiagram();
    assertEquals(MARKED, reloaded.getSource());
    startChangeMode(BLUE);
    specman.click(ACCEPT_CHANGES_BUTTON);
    assertFalse(reloaded.getSource().contains("blue"), reloaded.getSource());
  }

  /** Scenario: Deleting a diagram in change mode marks the diagram as deleted and leaves its source, with the marks
   * of its content, untouched. */
  @Test
  void testDeletingTheDiagramLeavesTheMarksAlone() {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(PINK);
    specman.focus(diagram);

    specman.press(KeyEvent.VK_DELETE);

    assertTrue(diagram.getChangeInfo().isDeleted(), "diagram must be marked as deleted");
    assertEquals(MARKED, diagram.getSource());
  }

  /** Scenario: The picture of a diagram which a change set has added stays white inside the frame, because the marks of
   * its content show what is new, and the pale color of the change set would make them harder to see. The rest of the
   * area has the color. */
  @Test
  void testAddedDiagramHasWhitePicture() {
    startChangeMode(BLUE);

    ClassmanImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());

    assertTrue(diagram.getChangeInfo().isAdded(), "diagram must be marked as added");
    assertEquals(BLUE.panelColor(), diagram.getBackground());
    assertEquals(BACKGROUND_COLOR_STANDARD, diagram.getPictureBackground());
  }

  /** Scenario: Accepting the addition takes the color and the white picture away: the diagram is an ordinary one. */
  @Test
  void testAcceptingTheAdditionLeavesNoColor() {
    startChangeMode(BLUE);
    ClassmanImageEditArea diagram = insertDiagramByToolbarButton(containerOfNewStep());

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(BACKGROUND_COLOR_STANDARD, diagram.getBackground());
    assertNull(diagram.getPictureBackground());
  }

  /** Scenario: A deleted diagram has the color of the change set behind its picture too, nothing in it says that it
   * is gone. */
  @Test
  void testDeletedDiagramHasColoredPicture() {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(PINK);
    specman.focus(diagram);

    specman.press(KeyEvent.VK_DELETE);

    assertEquals(PINK.panelColor(), diagram.getBackground());
    assertNull(diagram.getPictureBackground());
  }

  /** Scenario: A diagram nobody changed is saved as the compact one-line image block. */
  @Test
  void testUnchangedDiagramIsSavedAsOneLine(@TempDir Path directory) throws IOException {
    existingDiagram(containerOfNewStep());
    File file = directory.resolve("classman.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);

    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    assertTrue(saved.contains("{ `" + TWO_CLASSES + "` }"), "single line image block");
  }

  private static void startChangeMode(ChangeSet changeSet) {
    specman.toggleChangeMode();
    specman.selectChangeSet(changeSet);
  }

  /** The text which the serializer writes for a source, as the diagram has it after its marks were resolved. */
  private static String canonical(String source) throws ClassDiagramParseException {
    return new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));
  }

  private static ClassmanImageEditArea reloadedDiagram() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    EditContainer container = step.getFirstEditArea().asTextArea().getParent();
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }
}
