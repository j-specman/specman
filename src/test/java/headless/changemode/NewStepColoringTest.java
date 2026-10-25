package headless.changemode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ChangeSet;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchBereich;
import specman.view.EinfacherSchrittView;
import specman.view.SubsequenzSchrittView;

import static headless.HeadlessExtension.specman;
import static specman.DiagramToolBar.ACCEPT_CHANGES_BUTTON;

/**
 * Steps created in change mode are colored completely: All their texts are marked as changed and all areas
 * building up the step, including the ones of the substeps which come along, have the change set's panel color.
 * Accepting the changes removes the coloring completely again.
 * The test uses a change set other than the default one, so that a forgotten area can't pass by luck.
 */
class NewStepColoringTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  @ParameterizedTest
  @MethodSource("stepCreationButtons")
  void testNewStepIsColoredUntilAccepted(Class<? extends AbstractCreateStepOpButton> buttonType) {
    enableChangeMode(CHANGE_SET);
    specman.click(buttonType);
    AbstractSchrittView step = specman.findStepByStepID("1");
    assertColoredBy(step, CHANGE_SET);

    specman.click(ACCEPT_CHANGES_BUTTON);
    assertUncolored(step);
  }

  @Test
  void testNewCatchSequenceIsColoredUntilAccepted() {
    enableChangeMode(CHANGE_SET);
    SubsequenzSchrittView subsequence = specman.clickNewSubsequenceStep();
    specman.focus(specman.findSubstep(subsequence, "1.1", EinfacherSchrittView.class));
    BreakSchrittView breakStep = specman.clickNewBreakStep();
    specman.focus(subsequence);
    specman.choose(specman.clickCreateCatchButton(), breakStep);
    specman.assertStepNumbers("1", "1.1", "1.2", "1.2.1");
    CatchBereich catchBereich = subsequence.getParent().getCatchBereich();
    assertColoredBy(subsequence, CHANGE_SET);
    assertColoredBy(catchBereich, CHANGE_SET);

    specman.click(ACCEPT_CHANGES_BUTTON);
    assertUncolored(subsequence);
    assertUncolored(catchBereich);
  }
}
