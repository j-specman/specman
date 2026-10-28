package headless.changemode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ChangeSet;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchBereich;
import specman.view.EinfacherSchrittView;
import specman.view.SubsequenzSchrittView;

import static headless.HeadlessExtension.specman;

/**
 * Merging a change set makes all changes of the source change set changes of the current one: Everything which was
 * colored by the source set gets the current set's colors, in all steps, sequences, texts and catch areas.
 * The tests use two change sets other than the default one, so that a forgotten area can't pass by luck.
 */
class ChangeSetMergeTest extends AbstractChangemodeTest {

  private static final ChangeSet SOURCE_SET = ChangeSet.ALL.get("blue");
  private static final ChangeSet TARGET_SET = ChangeSet.ALL.get("orange");

  @ParameterizedTest
  @MethodSource("stepCreationButtons")
  void testStepAddedBySourceSetIsColoredByTargetSet(Class<? extends AbstractCreateStepOpButton> buttonType) {
    enableChangeMode(SOURCE_SET);
    specman.click(buttonType);
    AbstractSchrittView step = specman.findStepByStepID("1");
    assertColoredBy(step, SOURCE_SET);

    mergeSourceIntoTargetSet();

    assertColoredBy(step, TARGET_SET);
  }

  @Test
  void testCatchSequenceAddedBySourceSetIsColoredByTargetSet() {
    enableChangeMode(SOURCE_SET);
    SubsequenzSchrittView subsequence = specman.clickNewSubsequenceStep();
    specman.focus(specman.findSubstep(subsequence, "1.1", EinfacherSchrittView.class));
    BreakSchrittView breakStep = specman.clickNewBreakStep();
    specman.focus(subsequence);
    specman.choose(specman.clickCreateCatchButton(), breakStep);
    CatchBereich catchBereich = subsequence.getParent().getCatchBereich();
    assertColoredBy(subsequence, SOURCE_SET);
    assertColoredBy(catchBereich, SOURCE_SET);

    mergeSourceIntoTargetSet();

    assertColoredBy(subsequence, TARGET_SET);
    assertColoredBy(catchBereich, TARGET_SET);
  }

  /** The user switches to the target set and merges the source set into it. */
  private static void mergeSourceIntoTargetSet() {
    specman.selectChangeSet(TARGET_SET);
    specman.findComponent(MergeChangeSetsADBLOpButton.class).mergeChangeSets(SOURCE_SET);
  }
}
