package headless.changemode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static specman.DiagramToolBar.ACCEPT_CHANGES_BUTTON;

/** Deleting steps in change mode. Which way a step is deleted depends on whether it was created in change mode or not. */
class StepDeletionTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  /** A step which was added in change mode doesn't have to be tracked as deleted. It vanishes right away. */
  @Test
  void testStepAddedInChangeModeVanishesWhenDeleted() {
    specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    EinfacherSchrittView addedStep = specman.clickNewSimpleStep();
    specman.assertStepNumbers("1", "2");

    specman.focus(addedStep);
    specman.click(DeleteStepOpButton.class);

    specman.assertStepNumbers("1");
  }

  /** A step which was not created in change mode is still there after deleting it, but it is marked as deleted: Its
   * areas have the change set's color and its texts are struck through. Accepting the changes finally removes it.
   * A second step makes sure that the deleted one isn't the last one, which could not be deleted. */
  @ParameterizedTest
  @MethodSource("stepCreationButtons")
  void testStepFromBeforeChangeModeIsMarkedWhenDeleted(Class<? extends AbstractCreateStepOpButton> buttonType) {
    specman.click(buttonType);
    EinfacherSchrittView follower = specman.clickNewSimpleStep();
    AbstractSchrittView step = specman.findStepByStepID("1");
    enableChangeMode(CHANGE_SET);

    specman.focus(step);
    specman.click(DeleteStepOpButton.class);

    assertDeletedBy(step, CHANGE_SET);

    specman.click(ACCEPT_CHANGES_BUTTON);
    assertEquals(List.of(follower), specman.listAllSteps(), "accepting the deletion must remove the step with all its substeps");
  }
}
