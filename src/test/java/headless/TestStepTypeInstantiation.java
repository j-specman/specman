package headless;

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
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CaseSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.IfElseSchrittView;
import specman.view.IfSchrittView;
import specman.view.SubsequenzSchrittView;
import specman.view.WhileSchrittView;
import specman.view.WhileWhileSchrittView;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Click the button for each step type in an empty diagram and check that the matching step view
 * is there, together with the substeps it is expected to bring along.
 */
@ExtendWith(HeadlessExtension.class)
class TestStepTypeInstantiation {

  private SpecmanForTests specman;

  @Test
  void testSimpleStep() throws Exception {
    assertButtonCreatesStep(CreateSimpleStepOpButton.class, EinfacherSchrittView.class);
  }

  @Test
  void testWhileStep() throws Exception {
    WhileSchrittView step = assertButtonCreatesStep(CreateWhileStepOpButton.class, WhileSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
  }

  @Test
  void testWhileWhileStep() throws Exception {
    WhileWhileSchrittView step = assertButtonCreatesStep(CreateWhileWhileStepOpButton.class, WhileWhileSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
  }

  @Test
  void testIfElseStep() throws Exception {
    IfElseSchrittView step = assertButtonCreatesStep(CreateIfElseStepOpButton.class, IfElseSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
    assertSubstepExists(step, "2.1", EinfacherSchrittView.class);
  }

  @Test
  void testIfStep() throws Exception {
    IfSchrittView step = assertButtonCreatesStep(CreateIfStepOpButton.class, IfSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
  }

  @Test
  void testCaseStep() throws Exception {
    CaseSchrittView step = assertButtonCreatesStep(CreateCaseStepOpButton.class, CaseSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
    assertSubstepExists(step, "2.1", EinfacherSchrittView.class);
    assertSubstepExists(step, "3.1", EinfacherSchrittView.class);
  }

  @Test
  void testSubsequenceStep() throws Exception {
    SubsequenzSchrittView step = assertButtonCreatesStep(CreateSubsequenceStepOpButton.class, SubsequenzSchrittView.class);
    assertSubstepExists(step, "1.1", EinfacherSchrittView.class);
  }

  @Test
  void testBreakStep() throws Exception {
    assertButtonCreatesStep(CreateBreakStepOpButton.class, BreakSchrittView.class);
  }

  private <T extends AbstractSchrittView> T assertButtonCreatesStep(
      Class<? extends AbstractCreateStepOpButton> buttonType, Class<T> stepType) throws Exception {
    specman = new SpecmanForTests();
    specman.click(buttonType);
    return specman.findStepByStepID("1", stepType);
  }

  private <T extends AbstractSchrittView> T assertSubstepExists(AbstractSchrittView step, String stepID, Class<T> substepType) {
    T substep = specman.findStepByStepID(stepID, substepType);
    assertSame(step, substep.getParent().getParent(),
      "step " + stepID + " must be a substep of step " + step.getNumber());
    return substep;
  }
}
