package headless.saveload.scenario;

import org.junit.jupiter.api.Test;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.view.BreakSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.EinfacherSchrittView;
import specman.view.IfSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A catch sequence placed in the branch of an if step survives saving and loading. */
class CatchInIfBranchTest extends AbstractSaveLoadScenarioTest {

  @Test
  void testCatchSequenceInIfBranchSurvivesSavingAndLoading() {
    specman.click(CreateIfStepOpButton.class);
    IfSchrittView ifStep = specman.findStepByStepID("1", IfSchrittView.class);
    specman.focus(specman.findSubstep(ifStep, "1.1", EinfacherSchrittView.class));
    specman.click(CreateBreakStepOpButton.class);
    BreakSchrittView breakStep = specman.findStepByStepID("1.2", BreakSchrittView.class);
    specman.focus(specman.findStepByStepID("1.1", EinfacherSchrittView.class));
    specman.choose(specman.clickCreateCatchButton(), breakStep);

    saveAndReload("1", "1.1", "1.2", "1.2.1");

    EinfacherSchrittView catchStep = specman.findStepByStepID("1.2.1", EinfacherSchrittView.class);
    assertTrue(catchStep.getParent() instanceof CatchSchrittSequenzView);
  }
}
