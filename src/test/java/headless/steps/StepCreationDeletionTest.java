package headless.steps;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CaseSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.IfElseSchrittView;
import specman.view.IfSchrittView;
import specman.view.SubsequenzSchrittView;
import specman.view.WhileSchrittView;
import specman.view.WhileWhileSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * For each step type: Click the creation button in an empty diagram and check that the matching step
 * view is there, together with the substeps it is expected to bring along. Then append a simple
 * follower step, delete the first step again and check that the follower moves up to step number 1.
 */
@ExtendWith(HeadlessExtension.class)
class StepCreationDeletionTest {

  @Test
  void testSimpleStep() throws Exception {
    EinfacherSchrittView step = assertButtonCreatesStep(CreateSimpleStepOpButton.class, EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  @Test
  void testWhileStep() throws Exception {
    WhileSchrittView step = assertButtonCreatesStep(CreateWhileStepOpButton.class, WhileSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  @Test
  void testWhileWhileStep() throws Exception {
    WhileWhileSchrittView step = assertButtonCreatesStep(CreateWhileWhileStepOpButton.class, WhileWhileSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  @Test
  void testIfElseStep() throws Exception {
    IfElseSchrittView step = assertButtonCreatesStep(CreateIfElseStepOpButton.class, IfElseSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    specman.findSubstep(step, "2.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("3");
    assertStepDeletion(step, follower);
  }

  @Test
  void testIfStep() throws Exception {
    IfSchrittView step = assertButtonCreatesStep(CreateIfStepOpButton.class, IfSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  @Test
  void testCaseStep() throws Exception {
    CaseSchrittView step = assertButtonCreatesStep(CreateCaseStepOpButton.class, CaseSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    specman.findSubstep(step, "2.1", EinfacherSchrittView.class);
    specman.findSubstep(step, "3.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("4");
    assertStepDeletion(step, follower);
  }

  @Test
  void testSubsequenceStep() throws Exception {
    SubsequenzSchrittView step = assertButtonCreatesStep(CreateSubsequenceStepOpButton.class, SubsequenzSchrittView.class);
    specman.findSubstep(step, "1.1", EinfacherSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  @Test
  void testBreakStep() throws Exception {
    BreakSchrittView step = assertButtonCreatesStep(CreateBreakStepOpButton.class, BreakSchrittView.class);
    EinfacherSchrittView follower = assertFollowerStepNumber("2");
    assertStepDeletion(step, follower);
  }

  /** Clicks the given creation button in the empty diagram and returns the new step, which must be step 1. */
  private <T extends AbstractSchrittView> T assertButtonCreatesStep(
      Class<? extends AbstractCreateStepOpButton> buttonType, Class<T> stepType) {
    specman.click(buttonType);
    return specman.findStepByStepID("1", stepType);
  }

  /** After having created a step, the focus is placed in a way that another click on a
   * step creation button causes the next step to immediately follow the previous one.
   * @param stepNumber the follower step's expected number
   */
  private EinfacherSchrittView assertFollowerStepNumber(String stepNumber) {
    specman.click(CreateSimpleStepOpButton.class);
    return specman.findStepByStepID(stepNumber, EinfacherSchrittView.class);
  }

  /** Puts the cursor into the given step, clicks the delete button and checks that the
   * follower, which was step number 2 or higher before, has moved up to number 1.
   * @param step the step to delete, which must be step 1
   * @param follower the simple step following the step to delete
   */
  private void assertStepDeletion(AbstractSchrittView step, EinfacherSchrittView follower) {
    specman.focus(step);
    specman.click(DeleteStepOpButton.class);
    assertEquals("1", follower.getNumber().toString());
  }

}
