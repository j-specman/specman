package headless.changemode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import specman.ChangeSet;
import specman.ops.buttons.AbstractCreateStepOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.WhileSchrittView;

import java.util.stream.Stream;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.REVIEW_BUTTON;

/** The review button folds all steps without changes, so that the user gets to see the changes only. */
class ReviewCollapseTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  static Stream<Class<? extends AbstractCreateStepOpButton>> foldableStepCreationButtons() {
    return Stream.of(
      CreateWhileStepOpButton.class,
      CreateWhileWhileStepOpButton.class,
      CreateIfElseStepOpButton.class,
      CreateIfStepOpButton.class,
      CreateCaseStepOpButton.class,
      CreateSubsequenceStepOpButton.class);
  }

  @ParameterizedTest
  @MethodSource("foldableStepCreationButtons")
  void testEveryFoldableStepTypeWithoutChangesIsFolded(Class<? extends AbstractCreateStepOpButton> buttonType) {
    specman.click(buttonType);
    AbstractSchrittView step = specman.findStepByStepID("1");
    enableChangeMode(CHANGE_SET);

    specman.click(REVIEW_BUTTON);

    assertTrue(step.unterSequenzen().stream().noneMatch(sequence -> sequence.getShapeSequence() != null),
      "no sequence of the " + step.getClass().getSimpleName() + " may be visible");
  }

  @Test
  void testStepWithoutChangesIsFolded() {
    WhileSchrittView whileStep = createWhileStepWithCatchSequence();
    enableChangeMode(CHANGE_SET);

    specman.click(REVIEW_BUTTON);

    assertFalse(isUnfolded(whileStep));
  }

  /** Scenario: While step 1 contains the steps 1.1 and 1.2 (break) and has a catch sequence 1.2.1 for 1.2.
   * The user adds step 1.2.2 to the catch sequence in change mode. Besides this, there are no changes, but folding
   * the while step would hide the new step. */
  @Test
  void testStepWithChangesOnlyInCatchSequenceStaysUnfolded() {
    WhileSchrittView whileStep = createWhileStepWithCatchSequence();
    enableChangeMode(CHANGE_SET);
    specman.focus(specman.findStepByStepID("1.2.1"));
    specman.clickNewSimpleStep();
    specman.assertStepNumbers("1", "1.1", "1.2", "1.2.1", "1.2.2");

    specman.click(REVIEW_BUTTON);

    assertTrue(isUnfolded(whileStep));
  }

  /** Creates the diagram 1 (while) containing 1.1 (simple) and 1.2 (break) with the catch sequence 1.2.1 in the while step. */
  private static WhileSchrittView createWhileStepWithCatchSequence() {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    EinfacherSchrittView firstSubstep = specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class);
    specman.focus(firstSubstep);
    specman.click(CreateBreakStepOpButton.class);
    BreakSchrittView breakStep = specman.findStepByStepID("1.2", BreakSchrittView.class);
    specman.focus(firstSubstep);
    specman.choose(specman.clickCreateCatchButton(), breakStep);
    return whileStep;
  }

  /** The loop sequence is not part of the diagram's shape while the step is folded. */
  private static boolean isUnfolded(WhileSchrittView whileStep) {
    return whileStep.unterSequenzen().get(0).getShapeSequence() != null;
  }
}
