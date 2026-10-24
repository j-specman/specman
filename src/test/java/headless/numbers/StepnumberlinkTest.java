package headless.numbers;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.StepnumberLinkPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedElement;
import specman.ops.buttons.DeleteStepOpButton;
import specman.view.EinfacherSchrittView;
import specman.view.SubsequenzSchrittView;

import javax.swing.JOptionPane;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

/** Links to other steps: Inserting them via the step number link dialog and what happens to them when steps change. */
@ExtendWith(HeadlessExtension.class)
class StepnumberLinkTest {

  /** Scenario: Three simple steps, step 3 with an emptied text. In step 3, the user inserts a link to step 2 via
   * the step number link dialog, which must offer all three steps. Then a new step is inserted behind step 1.
   * The linked step becomes step 3, so the link text in the old step 3, now step 4, changes from "2" to "3". */
  @Test
  void testLinkToStepIsInsertedIntoFocusedText() {
    EinfacherSchrittView step1 = specman.createSimpleStep();
    EinfacherSchrittView step2 = specman.createSimpleStep();
    EinfacherSchrittView step3 = specman.createSimpleStep();
    TextEditArea step3Text = step3.getFirstEditArea().asTextArea();
    step3Text.setText("");

    specman.focus(step3);
    StepnumberLinkPanel panel = specman.doEditStepnumberLink();
    assertEquals(List.of(step1, step2, step3), panel.offeredSteps());

    assertFalse(step3Text.getPlainText().contains("2"));
    specman.choose(panel, step2);
    assertTrue(step3Text.getPlainText().contains("2"));

    specman.focus(step1);
    specman.createSimpleStep();
    specman.assertStepNumbers("1", "2", "3", "4");
    assertSame(step3, specman.findStepByStepID("4", EinfacherSchrittView.class));
    assertFalse(step3Text.getPlainText().contains("2"));
    assertTrue(step3Text.getPlainText().contains("3"));
  }

  /**
   * Scenario: Step 3 links to step 2. The user deletes step 2, which is referenced and therefore must be
   * confirmed. If the user confirms, the step is gone and the link in the former step 3 is marked as defect.
   * Otherwise step 2 and the link stay as they are.
   */
  @ParameterizedTest()
  @MethodSource("testDeletingReferencedStep_params")
  void testDeletingReferencedStep(
    int answer, boolean stepRemains, List<String> expectedStepNumbers, String expectedLink) {
    EinfacherSchrittView step2 = createStepsWithLinkFromStep3ToStep2();
    TextEditArea linkingText = specman.findStepByStepID("3", EinfacherSchrittView.class).getFirstEditArea().asTextArea();
    specman.answerConfirmDialogsWith(answer);

    specman.focus(step2);
    specman.click(DeleteStepOpButton.class);

    assertTrue(specman.shownMessages().contains(i18n("msg.delete.referenced.message")));
    assertEquals(stepRemains, specman.listAllSteps().contains(step2));
    assertEquals(expectedStepNumbers, specman.allStepNumbers());
    assertEquals(List.of(expectedLink), stepnumberLinks(linkingText));
  }

  static Stream<Arguments> testDeletingReferencedStep_params() {
    return Stream.of(
      Arguments.of(JOptionPane.OK_OPTION, false, List.of("1", "2"), "2?"),
      Arguments.of(JOptionPane.CANCEL_OPTION, true, List.of("1", "2", "3"), "2"));
  }

  /** Scenario: A simple step links to the first substep "2.1" of a subsequence step. A single Backspace behind
   * the link removes the complete reference, even though it consists of several characters. The link is then
   * also unregistered at the referenced step. */
  @Test
  void testSingleBackspaceRemovesWholeLink() {
    EinfacherSchrittView step1 = specman.createSimpleStep();
    TextEditArea step1Text = step1.getFirstEditArea().asTextArea();
    step1Text.setText("");

    SubsequenzSchrittView subsequence = specman.createSubsequenceStep();
    EinfacherSchrittView substep = specman.findSubstep(subsequence, "2.1", EinfacherSchrittView.class);

    specman.focus(step1);
    specman.choose(specman.doEditStepnumberLink(), substep);
    assertEquals("2.1", step1Text.getPlainText());
    assertTrue(substep.hasStepnumberLinks());

    step1Text.setCaretPosition(step1Text.getLength());
    specman.press(step1, KeyEvent.VK_BACK_SPACE);
    assertEquals("", step1Text.getPlainText()); // The whole "2.1" is gone
    assertFalse(substep.hasStepnumberLinks()); // No longer referenced
  }

  /** Creates three simple steps with a link in step 3 to step 2 and returns step 2. */
  private EinfacherSchrittView createStepsWithLinkFromStep3ToStep2() {
    specman.createSimpleStep();
    EinfacherSchrittView step2 = specman.createSimpleStep();
    EinfacherSchrittView step3 = specman.createSimpleStep();
    specman.focus(step3);
    specman.choose(specman.doEditStepnumberLink(), step2);
    return step2;
  }

  private static List<String> stepnumberLinks(TextEditArea text) {
    List<String> links = new ArrayList<>();
    for (WrappedElement link : text.findStepnumberLinks()) {
      links.add(text.getStepnumberLinkIDFromElement(link.getStartOffset(), link.getEndOffset()));
    }
    return links;
  }

}
