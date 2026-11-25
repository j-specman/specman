package specman;

import headless.changemode.AbstractChangemodeTest;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLAlignAction;
import org.junit.jupiter.api.Test;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import java.awt.event.KeyEvent;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Aligning text is a formatting only: It neither marks text as changed in change mode, nor does it remove
 * the marks which are there already. */
class AlignmentInChangeModeTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  @Test
  void aligningUntrackedTextInChangeModeMarksNothing() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    type(step, text, "abc");
    enableChangeMode(CHANGE_SET);
    AlignmentButtonsSupport.focus(text);

    AlignmentButtonsSupport.click(HTMLAlignAction.CENTER);

    assertAligned(text, "center");
    assertNoChangeMarkups(text);
  }

  @Test
  void aligningTextChangedInChangeModeKeepsItsMarkup() {
    enableChangeMode(CHANGE_SET);
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    type(step, text, "abc");
    assertWholeTextMarkedAsChangedBy(text, CHANGE_SET);
    AlignmentButtonsSupport.focus(text);

    AlignmentButtonsSupport.click(HTMLAlignAction.RIGHT);

    assertAligned(text, "right");
    assertEquals("abc", text.getPlainText());
    assertWholeTextMarkedAsChangedBy(text, CHANGE_SET);
  }

  private static void assertAligned(TextEditArea text, String alignment) {
    String html = text.getText().replaceAll("\s+", " ");
    assertTrue(html.contains("<div align=\"" + alignment + "\">"), html);
  }

  /** The key press makes the text area choose the change set's style for the following input, like in the real editor. */
  private static void type(EinfacherSchrittView step, TextEditArea text, String characters) {
    specman.press(step, KeyEvent.VK_A);
    text.selectAll();
    text.replaceSelection(characters);
  }
}
