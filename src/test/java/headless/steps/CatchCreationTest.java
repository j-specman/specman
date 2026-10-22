package headless.steps;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.CatchLinkPanel;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.view.BreakSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.EinfacherSchrittView;
import specman.view.WhileSchrittView;

import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A catch sequence is created for one of the break steps the catch link dialog offers.
 * Break steps which already have a catch sequence are not offered anymore.
 */
@ExtendWith(HeadlessExtension.class)
class CatchCreationTest {

  @Test
  void testCatchSequenceCreation() {
    WhileSchrittView whileStep = createWhileStepWithBreak();
    specman.assertStepNumbers("1", "1.1", "1.2");
    BreakSchrittView breakStep = specman.findStepByStepID("1.2", BreakSchrittView.class);

    specman.focus(whileStep);
    CatchLinkPanel catchLinkPanel = specman.clickCreateCatchButton();
    assertEquals(List.of(breakStep), catchLinkPanel.offeredBreakSteps());

    specman.choose(catchLinkPanel, breakStep);
    specman.assertStepNumbers("1", "1.1", "1.2", "1.2.1");
    EinfacherSchrittView catchStep = specman.findStepByStepID("1.2.1", EinfacherSchrittView.class);
    assertTrue(catchStep.getParent() instanceof CatchSchrittSequenzView);
    CatchSchrittSequenzView catchSchrittSequenzView = (CatchSchrittSequenzView) catchStep.getParent();

    specman.focus(whileStep);
    assertEquals(List.of(), specman.clickCreateCatchButton().offeredBreakSteps());
  }

  /** Creates the diagram 1 (while) containing 1.1 (simple) and 1.2 (break). */
  private WhileSchrittView createWhileStepWithBreak() {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    specman.focus(specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class));
    specman.click(CreateBreakStepOpButton.class);
    return whileStep;
  }

}
