package headless.changemode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.QuellSchrittView;
import specman.view.WhileSchrittView;

import java.util.List;
import java.util.stream.Stream;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.COPY_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.CUT_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.DELETE_ITEM;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.PASTE_ITEM;
import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;

/** The source step, which a step moved in change mode leaves behind at its old place. */
class SourceStepTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  private EinfacherSchrittView firstStep;
  private EinfacherSchrittView targetStep;
  private QuellSchrittView sourceStep;

  /** Two simple steps, the user moves step 2 in front of step 1 in change mode. The moved step gets number 1 and the
   * source step it leaves behind has number 3. */
  private void moveStepInChangeMode() throws Exception {
    firstStep = specman.clickNewSimpleStep();
    targetStep = specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    moveStep(targetStep, firstStep, Before);
    sourceStep = (QuellSchrittView) specman.findStepByStepID("3");
    assertTrue(sourceStep.getChangeInfo().isSourceStep());
    assertSame(sourceStep, targetStep.getQuellschritt());
  }

  /** Scenario: The source step is a placeholder without own content, so the user can't copy or cut it via the step
   * number's context menu. Deleting it and pasting behind it is fine. */
  @Test
  void testSourceStepCanNotBeCopiedOrCut() throws Exception {
    moveStepInChangeMode();

    assertFalse(specman.contextMenuItem(sourceStep, COPY_ITEM).isEnabled());
    assertFalse(specman.contextMenuItem(sourceStep, CUT_ITEM).isEnabled());
    assertTrue(specman.contextMenuItem(sourceStep, DELETE_ITEM).isEnabled());
    assertTrue(specman.contextMenuItem(sourceStep, PASTE_ITEM).isEnabled());
  }

  /** Scenario: The user deletes the source step of a move. Thereby the user decides to drop the information where the
   * moved step came from, so the target step becomes an ordinary step, in change mode as well as outside of it.
   * Undo brings back the source step and makes the moved step a target again, redo repeats the deletion. */
  @ParameterizedTest
  @MethodSource("changeModeOnAndOff")
  void testDeletingSourceStepMakesTargetStepOrdinary(boolean inChangeMode) throws Exception {
    moveStepInChangeMode();
    if (!inChangeMode) {
      specman.toggleChangeMode();
    }

    specman.clickContextMenuItem(sourceStep, DELETE_ITEM);

    assertDetachedFromSource();
    specman.undo();
    assertEquals(List.of(targetStep, firstStep, sourceStep), specman.listAllSteps());
    assertSame(sourceStep, targetStep.getQuellschritt());
    assertTrue(targetStep.getChangeInfo().isTargetStep());

    specman.redo();
    assertDetachedFromSource();
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: A loop 1 has the only step 1.1, which the user moves behind the simple step 2 in change mode. This
   * leaves the source step in the loop. Outside change mode the user deletes the loop with the source step in it.
   * The moved step 3 becomes an ordinary step. */
  @Test
  void testDeletingStepContainingSourceStepMakesTargetStepOrdinary() throws Exception {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    specman.focus(whileStep);
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    AbstractSchrittView innerStep = specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class);
    enableChangeMode(CHANGE_SET);
    moveStep(innerStep, step2, After);
    specman.toggleChangeMode();
    assertTrue(innerStep.getChangeInfo().isTargetStep());

    specman.clickContextMenuItem(whileStep, DELETE_ITEM);

    assertEquals(List.of(step2, innerStep), specman.listAllSteps());
    assertNull(innerStep.getQuellschritt());
    assertEquals(ChangeInfo.untracked(), innerStep.getChangeInfo());
  }

  private static Stream<Boolean> changeModeOnAndOff() {
    return Stream.of(true, false);
  }

  private void assertDetachedFromSource() {
    assertEquals(List.of(targetStep, firstStep), specman.listAllSteps());
    assertNull(targetStep.getQuellschritt());
    assertEquals(ChangeInfo.untracked(), targetStep.getChangeInfo());
  }
}
