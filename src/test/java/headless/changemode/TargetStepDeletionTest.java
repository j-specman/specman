package headless.changemode;

import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.QuellSchrittView;
import specman.view.WhileSchrittView;

import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.DELETE_ITEM;
import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;

/** Deleting the target of a move outside of change mode, i.e. while the move is not accepted yet. */
class TargetStepDeletionTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  /** Scenario: Two simple steps, the user moves step 2 in front of step 1 in change mode and switches the change
   * mode off again. Deleting the moved step 1 takes its source step 3 along, so the old place doesn't stay behind
   * as an orphan. Undo brings back both, redo removes both again. */
  @Test
  void testDeletingTargetStepRemovesSourceStepAsWell() throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    moveStep(step2, step1, Before);
    QuellSchrittView sourceStep = (QuellSchrittView) specman.findStepByStepID("3");
    specman.toggleChangeMode();

    specman.clickContextMenuItem(step2, DELETE_ITEM);

    specman.assertStepNumbers("1");
    assertEquals(List.of(step1), specman.listAllSteps());

    specman.undo();
    specman.assertStepNumbers("1", "2", "3");
    assertEquals(List.of(step2, step1, sourceStep), specman.listAllSteps());
    assertTrue(sourceStep.getChangeInfo().isSourceStep());

    specman.redo();
    assertEquals(List.of(step1), specman.listAllSteps());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: A loop 1 with step 1.1 is followed by simple steps 2 and 3. In change mode the user moves step 2 into
   * the loop behind step 1.1 and switches the change mode off again. Deleting the loop takes the source step
   * the moved step left behind outside the loop along, so only the last simple step remains. Undo brings back all of
   * it in the old order, redo removes it again. */
  @Test
  void testDeletingStepRemovesSourceStepsOfTargetStepsInside() throws Exception {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    specman.focus(whileStep);
    EinfacherSchrittView movedStep = specman.clickNewSimpleStep();
    EinfacherSchrittView lastStep = specman.clickNewSimpleStep();
    AbstractSchrittView innerStep = specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class);
    enableChangeMode(CHANGE_SET);
    moveStep(movedStep, innerStep, After);
    specman.toggleChangeMode();
    List<AbstractSchrittView> stepsBefore = List.copyOf(specman.listAllSteps());
    assertEquals(5, stepsBefore.size(), "loop, its two steps and the source step, which is followed by the last step");

    specman.clickContextMenuItem(whileStep, DELETE_ITEM);

    assertEquals(List.of(lastStep), specman.listAllSteps());

    specman.undo();
    assertEquals(stepsBefore, specman.listAllSteps());

    specman.redo();
    assertEquals(List.of(lastStep), specman.listAllSteps());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The only step 1.1 of a loop is moved out of it behind step 2 in change mode, which leaves its source
   * step as the only step in the loop. Outside of change mode, deleting the moved step would empty the loop by
   * taking the source step along. This is refused like deleting the last step of any sequence. */
  @Test
  void testDeletingTargetStepIsRefusedIfItEmptiesTheSequenceOfTheSourceStep() throws Exception {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    specman.focus(whileStep);
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    AbstractSchrittView innerStep = specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class);
    enableChangeMode(CHANGE_SET);
    moveStep(innerStep, step2, After);
    specman.toggleChangeMode();
    List<AbstractSchrittView> stepsBefore = List.copyOf(specman.listAllSteps());

    assertFalse(specman.contextMenuItem(innerStep, DELETE_ITEM).isEnabled());
    specman.focus(innerStep);
    specman.click(DeleteStepOpButton.class);

    assertEquals(stepsBefore, specman.listAllSteps());
    assertEquals(List.of(i18n("msg.delete.last.step")), specman.shownMessages());
  }
}
