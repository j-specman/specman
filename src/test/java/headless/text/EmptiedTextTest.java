package headless.text;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import java.awt.event.KeyEvent;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An empty text consists of an invisible newline. A text document of length 0 breaks the wrapped
 * positions, so whatever the user does to remove text must never produce one.
 */
class EmptiedTextTest extends AbstractTextTest {

  private static final int EMPTY_TEXT_LENGTH = 1;

  private EinfacherSchrittView step;
  private TextEditArea text;

  @BeforeEach
  void createStepWithText() {
    step = specman.createSimpleStep();
    text = textAreaOf(step);
    specman.focus(step);
  }

  @Test
  void testBackspaceNeverRemovesInvisibleNewline() {
    text.setCaretPosition(length());
    pressRepeatedly(KeyEvent.VK_BACK_SPACE, length() + 5);

    assertEmpty();
  }

  @Test
  void testDeleteNeverRemovesInvisibleNewline() {
    text.setCaretPosition(0);
    pressRepeatedly(KeyEvent.VK_DELETE, length() + 5);

    assertEmpty();
  }

  @Test
  void testDeleteAtTextStartRemovesFirstVisibleCharacter() {
    String initialText = text.getPlainText();
    text.setCaretPosition(0);

    specman.press(step, KeyEvent.VK_DELETE);

    assertEquals(initialText.substring(1), text.getPlainText());
  }

  @Test
  void testBackspaceOnSelectedAllTextKeepsInvisibleNewline() {
    text.selectAll();

    specman.press(step, KeyEvent.VK_BACK_SPACE);

    assertEmpty();
  }

  @Test
  void testDeleteOnSelectedAllTextKeepsInvisibleNewline() {
    text.selectAll();

    specman.press(step, KeyEvent.VK_DELETE);

    assertEmpty();
  }

  @Test
  void testSettingEmptyTextKeepsInvisibleNewline() {
    text.setText("");

    assertEmpty();
  }

  private int length() {
    return text.getDocument().getLength();
  }

  private void pressRepeatedly(int keyCode, int times) {
    for (int i = 0; i < times; i++) {
      specman.press(step, keyCode);
    }
  }

  private void assertEmpty() {
    assertTrue(text.getPlainText().isBlank());
    assertEquals(EMPTY_TEXT_LENGTH, length());
  }
}
