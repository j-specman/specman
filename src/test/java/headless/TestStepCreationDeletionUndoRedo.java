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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * For each step type: Undo and redo the creation of the step, then undo its deletion.
 * An empty diagram is not allowed, so each test starts with a simple base step as step 1. The step
 * under test is created as step 2, so the numbers of its substeps are shifted accordingly.
 */
@ExtendWith(HeadlessExtension.class)
class TestStepCreationDeletionUndoRedo {

  private SpecmanForTests specman;

  @Test
  void testSimpleStep() throws Exception {
    assertUndoRedo(CreateSimpleStepOpButton.class, EinfacherSchrittView.class);
  }

  @Test
  void testWhileStep() throws Exception {
    assertUndoRedo(CreateWhileStepOpButton.class, WhileSchrittView.class, "2.1");
  }

  @Test
  void testWhileWhileStep() throws Exception {
    assertUndoRedo(CreateWhileWhileStepOpButton.class, WhileWhileSchrittView.class, "2.1");
  }

  @Test
  void testIfElseStep() throws Exception {
    assertUndoRedo(CreateIfElseStepOpButton.class, IfElseSchrittView.class, "2.1", "3.1");
  }

  @Test
  void testIfStep() throws Exception {
    assertUndoRedo(CreateIfStepOpButton.class, IfSchrittView.class, "2.1");
  }

  @Test
  void testCaseStep() throws Exception {
    assertUndoRedo(CreateCaseStepOpButton.class, CaseSchrittView.class, "2.1", "3.1", "4.1");
  }

  @Test
  void testSubsequenceStep() throws Exception {
    assertUndoRedo(CreateSubsequenceStepOpButton.class, SubsequenzSchrittView.class, "2.1");
  }

  @Test
  void testBreakStep() throws Exception {
    assertUndoRedo(CreateBreakStepOpButton.class, BreakSchrittView.class);
  }

  /**
   * @param substepIds the numbers of the simple substeps the step under test is expected to have
   */
  private <T extends AbstractSchrittView> void assertUndoRedo(
      Class<? extends AbstractCreateStepOpButton> buttonType, Class<T> stepType, String... substepIds) throws Exception {
    specman = new SpecmanForTests();
    specman.click(CreateSimpleStepOpButton.class);
    specman.click(buttonType);
    T step = assertStepWithSubsteps(stepType, substepIds);

    specman.undo();
    assertOnlyBaseStep();

    specman.redo();
    step = assertStepWithSubsteps(stepType, substepIds);

    specman.focus(step);
    specman.click(DeleteStepOpButton.class);
    assertOnlyBaseStep();

    specman.undo();
    assertStepWithSubsteps(stepType, substepIds);
  }

  private <T extends AbstractSchrittView> T assertStepWithSubsteps(Class<T> stepType, String... substepIds) {
    T step = specman.findStepByStepID("2", stepType);
    for (String substepId : substepIds) {
      specman.findSubstep(step, substepId, EinfacherSchrittView.class);
    }
    return step;
  }

  private void assertOnlyBaseStep() {
    assertNull(specman.findStepByStepID("2"), "step 2 must not exist");
    assertEquals(1, specman.listAllSteps().size(), "only the base step must be left");
  }
}
