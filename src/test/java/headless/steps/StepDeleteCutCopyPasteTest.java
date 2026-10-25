package headless.steps;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CaseSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.IfElseSchrittView;
import specman.view.IfSchrittView;
import specman.view.SubsequenzSchrittView;
import specman.view.WhileSchrittView;
import specman.view.WhileWhileSchrittView;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static headless.HeadlessExtension.specman;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.COPY_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.CUT_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.DELETE_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.PASTE_ITEM;

/** Deleting, copying and cutting steps via the step number's context menu and pasting them elsewhere. */
@ExtendWith(HeadlessExtension.class)
class StepDeleteCutCopyPasteTest {

  /**
   * Scenario: Step 1 is of the type under test, followed by a simple step 2. The user copies step 1 and pastes it
   * behind step 2. The pasted step is a complete copy of step 1 including its substeps and its text, but all
   * pasted steps have new IDs. Step 1 and step 2 stay as they are.
   */
  @ParameterizedTest
  @MethodSource("testCopyPasteBehindOtherStep_params")
  void testCopyPasteBehindOtherStep(
      Class<? extends AbstractCreateStepOpButton> buttonType, Class<? extends AbstractSchrittView> stepType,
      List<String> expectedStepNumbers) {
    specman.click(buttonType);
    AbstractSchrittView original = specman.findStepByStepID("1", stepType);
    String originalText = original.getFirstEditArea().asTextArea().getPlainText();
    specman.focus(original);
    EinfacherSchrittView simpleStep = specman.clickNewSimpleStep();
    List<AbstractSchrittView> stepsBeforePaste = List.copyOf(specman.listAllSteps());
    int originalSize = stepsBeforePaste.size() - 1; // The original step with its substeps, i.e. all but the simple step

    specman.clickContextMenuItem(original, COPY_ITEM);
    specman.clickContextMenuItem(simpleStep, PASTE_ITEM);

    assertEquals(List.of(), specman.shownMessages());
    specman.assertStepNumbers(expectedStepNumbers);
    List<AbstractSchrittView> steps = specman.listAllSteps();
    assertEquals(stepsBeforePaste, steps.subList(0, stepsBeforePaste.size()), "existing steps must be untouched");
    List<AbstractSchrittView> pasted = steps.subList(stepsBeforePaste.size(), steps.size());
    assertEquals(viewTypes(stepsBeforePaste.subList(0, originalSize)), viewTypes(pasted));
    assertNotSame(original, pasted.get(0));
    assertNotEquals(original.getId(), pasted.get(0).getId());
    Set<String> existingIds = stepsBeforePaste.stream().map(AbstractSchrittView::getId).collect(toSet());
    pasted.forEach(step -> assertFalse(existingIds.contains(step.getId()), "pasted step " + step.getNumber() + " must have a new ID"));
    assertEquals(originalText, pasted.get(0).getFirstEditArea().asTextArea().getPlainText());
  }

  /** Scenario: Two simple steps, the user deletes step 1 via the context menu. The remaining step moves up to number 1.
   * Undo restores the deleted step in its old place, redo deletes it again. */
  @Test
  void testDeleteUndoRedo() {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    String deletedText = step1.getFirstEditArea().asTextArea().getPlainText();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    String stayText = step2.getFirstEditArea().asTextArea().getPlainText();
    assertNotEquals(deletedText, stayText, "the steps must be distinguishable by their texts");

    specman.clickContextMenuItem(step1, DELETE_ITEM);
    specman.assertStepNumbers(List.of("1"));
    assertStepTexts(stayText);

    specman.undo();
    specman.assertStepNumbers(List.of("1", "2"));
    assertStepTexts(deletedText, stayText);

    specman.redo();
    specman.assertStepNumbers(List.of("1"));
    assertStepTexts(stayText);
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: Two simple steps, the user copies step 1 and pastes it behind step 2. Undoing removes the pasted
   * step 3 only, the copy action itself is not part of the undo history. Redo brings the pasted step back with its text. */
  @Test
  void testPasteUndoRedo() {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    String originalText = step1.getFirstEditArea().asTextArea().getPlainText();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    specman.clickContextMenuItem(step1, COPY_ITEM);
    specman.clickContextMenuItem(step2, PASTE_ITEM);
    specman.assertStepNumbers(List.of("1", "2", "3"));

    specman.undo();
    specman.assertStepNumbers(List.of("1", "2"));

    specman.redo();
    specman.assertStepNumbers(List.of("1", "2", "3"));
    EinfacherSchrittView pasted = specman.findStepByStepID("3", EinfacherSchrittView.class);
    assertEquals(originalText, pasted.getFirstEditArea().asTextArea().getPlainText());
  }

  /** Scenario: Two simple steps, the user cuts step 1 and pastes it behind the remaining step, which is step 1 now.
   * Undo removes the pasted step, a second undo restores the cut step in its old place. Redo repeats both. */
  @Test
  void testCutPasteUndoRedo() {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    String cutText = step1.getFirstEditArea().asTextArea().getPlainText();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    String stayText = step2.getFirstEditArea().asTextArea().getPlainText();
    assertNotEquals(cutText, stayText, "the steps must be distinguishable by their texts");

    specman.clickContextMenuItem(step1, CUT_ITEM);
    specman.assertStepNumbers(List.of("1"));
    specman.clickContextMenuItem(step2, PASTE_ITEM);
    assertStepTexts(stayText, cutText);

    specman.undo();
    assertStepTexts(stayText);

    specman.undo();
    assertStepTexts(cutText, stayText);

    specman.redo();
    assertStepTexts(stayText);

    specman.redo();
    assertStepTexts(stayText, cutText);
    assertEquals(List.of(), specman.shownMessages());
  }

  private static void assertStepTexts(String... expectedTexts) {
    List<String> texts = specman.listAllSteps().stream()
      .map(step -> step.getFirstEditArea().asTextArea().getPlainText())
      .toList();
    assertEquals(List.of(expectedTexts), texts);
  }

  static Stream<Arguments> testCopyPasteBehindOtherStep_params() {
    return Stream.of(
      Arguments.of(CreateSimpleStepOpButton.class, EinfacherSchrittView.class, List.of("1", "2", "3")),
      Arguments.of(CreateWhileStepOpButton.class, WhileSchrittView.class, List.of("1", "1.1", "2", "3", "3.1")),
      Arguments.of(CreateWhileWhileStepOpButton.class, WhileWhileSchrittView.class, List.of("1", "1.1", "2", "3", "3.1")),
      Arguments.of(CreateIfElseStepOpButton.class, IfElseSchrittView.class, List.of("1", "1.1", "2.1", "3", "4", "4.1", "5.1")),
      Arguments.of(CreateIfStepOpButton.class, IfSchrittView.class, List.of("1", "1.1", "2", "3", "3.1")),
      Arguments.of(CreateCaseStepOpButton.class, CaseSchrittView.class,
        List.of("1", "2.1", "3.1", "1.1", "4", "5", "6.1", "7.1", "5.1")),
      Arguments.of(CreateSubsequenceStepOpButton.class, SubsequenzSchrittView.class, List.of("1", "1.1", "2", "3", "3.1")),
      Arguments.of(CreateBreakStepOpButton.class, BreakSchrittView.class, List.of("1", "2", "3")));
  }

  private static List<Class<?>> viewTypes(List<AbstractSchrittView> steps) {
    return steps.stream().<Class<?>>map(Object::getClass).toList();
  }
}
