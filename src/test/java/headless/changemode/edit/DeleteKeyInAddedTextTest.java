package headless.changemode.edit;

import headless.changemode.AbstractChangemodeTest;
import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import java.awt.event.KeyEvent;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Delete removes characters which have been added in change mode for real, while it only marks other characters as
 * deleted. In the first case the caret stays where it is, in the second one it moves behind the marked character.
 */
class DeleteKeyInAddedTextTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");
  /** The text document starts with an invisible newline, so the plain text starts at document offset 1. */
  private static final int AFTER_FIRST_CHARACTER = 2;

  @Test
  void testDeleteInAddedTextKeepsCaretPosition() {
    enableChangeMode(CHANGE_SET);
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    type(step, text, "abc");
    assertWholeTextMarkedAsChangedBy(text, CHANGE_SET);
    text.setCaretPosition(AFTER_FIRST_CHARACTER);

    specman.press(step, KeyEvent.VK_DELETE);

    assertEquals("ac", text.getPlainText());
    assertEquals(AFTER_FIRST_CHARACTER, text.getCaretPosition());
  }

  @Test
  void testDeleteInUntrackedTextMarksCharacterAsDeletedAndMovesCaretBehindIt() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    type(step, text, "abc");
    enableChangeMode(CHANGE_SET);
    text.setCaretPosition(AFTER_FIRST_CHARACTER);

    specman.press(step, KeyEvent.VK_DELETE);

    assertEquals("abc", text.getPlainText());
    assertEquals(AFTER_FIRST_CHARACTER + 1, text.getCaretPosition());
  }

  /** The key press makes the text area choose the change set's style for the following input, like in the real editor. */
  private static void type(EinfacherSchrittView step, TextEditArea text, String characters) {
    specman.press(step, KeyEvent.VK_A);
    text.selectAll();
    text.replaceSelection(characters);
  }
}
