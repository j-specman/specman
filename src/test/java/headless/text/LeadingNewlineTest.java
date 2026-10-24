package headless.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import specman.editarea.TextEditArea;

import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every text starts with an invisible newline, which WrappedDocument relies on. Swing's HTMLEditorKit
 * produces it by itself, so the first test pins that assumption in case a JDK update changes it.
 * The others check that it stays in place. */
class LeadingNewlineTest extends AbstractTextTest {

  @ParameterizedTest(name = "html: {0}")
  @ValueSource(strings = {
    "", "x", "abc", "<p>abc</p>", "<p></p>", "<p>a</p><p>b</p>", "<ul><li>a</li><li>b</li></ul>", "<ol><li>a</li></ol>",
    "<h1>head</h1>", "<div>abc</div>", "<div align='center'>abc</div>", "<p align='right'>abc</p>", "<b>abc</b>",
    "a<br>b", "<pre>x</pre>", "<blockquote>q</blockquote>", " leading space",
    "<html><head></head><body>abc</body></html>", "<html><head></head><body><ul><li>a</li></ul></body></html>"
  })
  void testEveryKindOfTextStartsWithInvisibleNewline(String html) throws BadLocationException {
    assertTrue(rawText(textArea(html)).startsWith("\n"));
  }

  @Test
  void testTextInsertedAtDocumentStartLandsBehindNewline() throws BadLocationException {
    TextEditArea text = textArea("abc");

    text.getDocument().insertString(0, "x", null);

    assertEquals("\nxabc", rawText(text));
  }

  @Test
  void testSettingTextKeepsNewlineAndPutsCaretBehindIt() throws BadLocationException {
    TextEditArea text = textArea("abc");

    text.setText("<b>new</b> text");

    assertEquals("\nnew text", rawText(text));
    assertEquals(1, text.getCaretPosition());
  }

  @Test
  void testCaretCannotBePlacedBeforeNewline() {
    TextEditArea text = textArea("abc");

    text.setCaretPosition(0);

    assertEquals(1, text.getCaretPosition());
  }

  @Test
  void testSelectAllSelectsOnlyVisibleText() {
    TextEditArea text = textArea("abc");

    text.selectAll();

    assertEquals(1, text.getSelectionStart());
    assertEquals(text.getDocument().getLength(), text.getSelectionEnd());
  }

  private static String rawText(TextEditArea text) throws BadLocationException {
    return text.getDocument().getText(0, text.getDocument().getLength());
  }
}
