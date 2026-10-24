package headless.numbers;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.StepnumberLinkPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.TextEditArea;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.view.EinfacherSchrittView;

import java.awt.event.KeyEvent;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Inserting links to other steps into a text via the step number link dialog. */
@ExtendWith(HeadlessExtension.class)
class StepnumberLinkTest {

  @Test
  void testLinkToStepIsInsertedIntoFocusedText() {
    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step1 = specman.findStepByStepID("1", EinfacherSchrittView.class);

    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step2 = specman.findStepByStepID("2", EinfacherSchrittView.class);

    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step3 = specman.findStepByStepID("3", EinfacherSchrittView.class);
    TextEditArea step3Text = step3.getFirstEditArea().asTextArea();
    step3Text.setText("#");

    specman.focus(step3);
    StepnumberLinkPanel panel = specman.doEditStepnumberLink();
    assertEquals(List.of(step1, step2, step3), panel.offeredSteps());
    assertFalse(step3Text.getPlainText().contains("2"));

    specman.choose(panel, step2);
    assertTrue(step3Text.getPlainText().contains("2"));

    specman.focus(step1);
    specman.click(CreateSimpleStepOpButton.class);
    specman.assertStepNumbers("1", "2", "3", "4");
    assertSame(step3, specman.findStepByStepID("4", EinfacherSchrittView.class));
    assertFalse(step3Text.getPlainText().contains("2"));
    assertTrue(step3Text.getPlainText().contains("3"));
  }

  /** The user keeps pressing Backspace after having deleted the complete text. Linking must still work afterwards. */
  @Test
  void testLinkIntoTextEmptiedByBackspaceSurvivesRenumbering() {
    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step1 = specman.findStepByStepID("1", EinfacherSchrittView.class);
    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step2 = specman.findStepByStepID("2", EinfacherSchrittView.class);
    specman.click(CreateSimpleStepOpButton.class);
    EinfacherSchrittView step3 = specman.findStepByStepID("3", EinfacherSchrittView.class);
    TextEditArea step3Text = step3.getFirstEditArea().asTextArea();

    specman.focus(step3);
    int textLength = step3Text.getDocument().getLength();
    step3Text.setCaretPosition(textLength);
    for (int i = 0; i < textLength + 5; i++) {
      specman.press(step3, KeyEvent.VK_BACK_SPACE);
    }
    assertTrue(step3Text.getPlainText().isBlank());

    specman.choose(specman.doEditStepnumberLink(), step2);
    assertTrue(step3Text.getPlainText().contains("2"));

    specman.focus(step1);
    specman.click(CreateSimpleStepOpButton.class);
    assertFalse(step3Text.getPlainText().contains("2"));
    assertTrue(step3Text.getPlainText().contains("3"));
  }
}
