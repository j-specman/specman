package headless.changemode;

import org.junit.jupiter.api.Test;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.TextEditArea;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseBranchADBLOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CaseSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.EinfacherSchrittView;
import specman.view.QuellSchrittView;
import specman.view.WhileSchrittView;
import specman.view.ZweigSchrittSequenzView;
import specman.visitor.TextAreaCollector;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.view.RelativeStepPosition.After;

/** Deleting branches of case steps and catch sequences outside of change mode while they take part in a move which
 * is not accepted yet: Wherever a source or target step vanishes, the move is void. */
class MoveTieDeletionInSequencesTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  /** Scenario: A case step 1 gets a third case branch, the case step is followed by the simple step 4. In change mode
   * the user moves step 4 into the new branch and switches change mode off. Deleting the branch takes the source
   * step 4 left behind in the main sequence along. Undo brings back the branch and both steps. */
  @Test
  void testDeletingCaseBranchRemovesSourceStepsOfTargetStepsInside() throws Exception {
    CaseSchrittView caseStep = createCaseStepWithThirdBranch();
    EinfacherSchrittView movedStep = specman.clickNewSimpleStep();
    ZweigSchrittSequenzView branch = caseStep.getCaseSequenzen().get(1);
    enableChangeMode(CHANGE_SET);
    moveStep(movedStep, branch.getSchritte().get(0), After);
    QuellSchrittView sourceStep = movedStep.getQuellschritt();
    specman.toggleChangeMode();

    deleteBranch(caseStep, branch);

    assertFalse(specman.listAllSteps().contains(movedStep));
    assertFalse(specman.listAllSteps().contains(sourceStep));

    specman.undo();
    assertTrue(specman.listAllSteps().contains(movedStep));
    assertTrue(specman.listAllSteps().contains(sourceStep));
    assertSame(sourceStep, movedStep.getQuellschritt());
    assertTrue(movedStep.getChangeInfo().isTargetStep());
  }

  /** Scenario: Same case step, but the user moves the step of the new branch out of the case step in change mode.
   * This leaves the source step in the branch. Deleting the branch outside change mode makes the moved step an
   * ordinary step. */
  @Test
  void testDeletingCaseBranchMakesTargetStepsOfSourceStepsInsideOrdinary() throws Exception {
    CaseSchrittView caseStep = createCaseStepWithThirdBranch();
    ZweigSchrittSequenzView branch = caseStep.getCaseSequenzen().get(1);
    AbstractSchrittView branchStep = branch.getSchritte().get(0);
    enableChangeMode(CHANGE_SET);
    moveStep(branchStep, caseStep, After);
    assertTrue(branchStep.getChangeInfo().isTargetStep());
    specman.toggleChangeMode();

    deleteBranch(caseStep, branch);

    assertNull(branchStep.getQuellschritt());
    assertEquals(ChangeInfo.untracked(), branchStep.getChangeInfo());
    assertTrue(specman.listAllSteps().contains(branchStep));
  }

  /** Scenario: The catch sequence of a break step in a loop 1 has the only step 1.2.1, which the user moves behind
   * the loop in change mode. The source step stays in the catch sequence. Outside change mode the user deletes the
   * break step, which takes the catch sequence with the source step along. The moved step becomes an ordinary step. */
  @Test
  void testDeletingBreakStepRemovesItsCatchSequenceWithItsSourceSteps() throws Exception {
    CatchSetup setup = createLoopWithCatchSequenceAndMoveCatchStepOut();

    specman.focus(setup.breakStep);
    specman.click(DeleteStepOpButton.class);

    assertFalse(specman.listAllSteps().contains(setup.sourceStep));
    assertNull(setup.movedStep.getQuellschritt());
    assertEquals(ChangeInfo.untracked(), setup.movedStep.getChangeInfo());
  }

  /** Scenario: Same as above, but the user deletes the catch sequence itself via its heading. */
  @Test
  void testDeletingCatchSequenceMakesTargetStepsOfSourceStepsInsideOrdinary() throws Exception {
    CatchSetup setup = createLoopWithCatchSequenceAndMoveCatchStepOut();
    TextAreaCollector headingCollector = new TextAreaCollector();
    setup.catchSequence.traverse(headingCollector);
    TextEditArea catchHeading = headingCollector.textAreas().get(0);

    specman.focus(catchHeading);
    specman.click(DeleteStepOpButton.class);

    assertFalse(specman.listAllSteps().contains(setup.sourceStep));
    assertNull(setup.movedStep.getQuellschritt());
    assertEquals(ChangeInfo.untracked(), setup.movedStep.getChangeInfo());
  }

  private static CaseSchrittView createCaseStepWithThirdBranch() {
    specman.click(CreateCaseStepOpButton.class);
    CaseSchrittView caseStep = specman.findStepByStepID("1", CaseSchrittView.class);
    specman.focus(caseStep.getCaseSequenzen().get(0).getUeberschrift().getFirstEditArea());
    specman.click(CreateCaseBranchADBLOpButton.class);
    assertEquals(3, caseStep.getCaseSequenzen().size());
    specman.focus(caseStep);
    return caseStep;
  }

  private static void deleteBranch(CaseSchrittView caseStep, ZweigSchrittSequenzView branch) {
    specman.focus(branch.getUeberschrift().getFirstEditArea());
    specman.click(DeleteStepOpButton.class);
    assertFalse(caseStep.getCaseSequenzen().contains(branch), "the branch must be gone");
  }

  private record CatchSetup(BreakSchrittView breakStep, CatchSchrittSequenzView catchSequence,
      EinfacherSchrittView movedStep, QuellSchrittView sourceStep) {}

  /** Creates the loop 1 with the simple step 1.1, the break step 1.2 and its catch sequence 1.2.1, followed by
   * the simple step 2. In change mode, step 1.2.1 is moved behind step 2 and the change mode is switched off. */
  private CatchSetup createLoopWithCatchSequenceAndMoveCatchStepOut() throws Exception {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    specman.focus(specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class));
    specman.click(CreateBreakStepOpButton.class);
    BreakSchrittView breakStep = specman.findStepByStepID("1.2", BreakSchrittView.class);
    specman.focus(specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class));
    specman.choose(specman.clickCreateCatchButton(), breakStep);
    EinfacherSchrittView catchStep = specman.findStepByStepID("1.2.1", EinfacherSchrittView.class);
    CatchSchrittSequenzView catchSequence = (CatchSchrittSequenzView) catchStep.getParent();
    specman.focus(whileStep);
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    moveStep(catchStep, step2, After);
    specman.toggleChangeMode();
    return new CatchSetup(breakStep, catchSequence, catchStep, catchStep.getQuellschritt());
  }
}
