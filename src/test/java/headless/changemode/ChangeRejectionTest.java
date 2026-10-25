package headless.changemode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ChangeSet;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.IfSchrittView;

import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static specman.DiagramToolBar.REJECT_CHANGES_BUTTON;

/** Rejecting the changes made in change mode. */
class ChangeRejectionTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  /** Scenario: A step created in change mode is gone after rejecting the changes. The step which already existed before
   * stays untouched. Since all step types are added the same way, a simple step is sufficient to test this. */
  @Test
  void testAddedStepVanishesWhenRejected() {
    EinfacherSchrittView existingStep = specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    specman.clickNewSimpleStep();
    specman.assertStepNumbers("1", "2");

    specman.click(REJECT_CHANGES_BUTTON);

    assertEquals(List.of(existingStep), specman.listAllSteps());
    specman.assertStepNumbers("1");
  }

  /** Scenario: A step created before the change mode is deleted in change mode, which marks it as deleted. Rejecting
   * the changes takes the deletion back. Details of the step's coloring matter and are individual for each step
   * type, so all of them are tested. A second step makes sure that the deleted one isn't the last one. */
  @ParameterizedTest
  @MethodSource("stepCreationButtons")
  void testDeletedStepIsRestoredWhenRejected(Class<? extends AbstractCreateStepOpButton> buttonType) {
    specman.click(buttonType);
    specman.clickNewSimpleStep();
    AbstractSchrittView step = specman.findStepByStepID("1");
    enableChangeMode(CHANGE_SET);
    specman.focus(step);
    specman.click(DeleteStepOpButton.class);
    assertDeletedBy(step, CHANGE_SET);

    specman.click(REJECT_CHANGES_BUTTON);

    assertUncolored(step);
  }

  /** Scenario: The empty heading of an if step is not editable by design. This stays so after the deletion of the
   * step has been rejected, which makes all other texts of the step editable again. */
  @Test
  void testIfStepHeadingStaysNotEditableWhenDeletionIsRejected() {
    specman.click(CreateIfStepOpButton.class);
    specman.clickNewSimpleStep();
    IfSchrittView ifStep = specman.findStepByStepID("1", IfSchrittView.class);
    enableChangeMode(CHANGE_SET);
    specman.focus(ifStep);
    specman.click(DeleteStepOpButton.class);
    specman.click(REJECT_CHANGES_BUTTON);

    assertFalse(ifStep.getIfSequenz().getUeberschrift().getFirstEditArea().asTextArea().isEditable());
  }
}
