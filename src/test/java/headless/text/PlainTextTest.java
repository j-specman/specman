package headless.text;

import org.junit.jupiter.api.Test;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import java.awt.image.BufferedImage;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The plain text of an edit area contains only what the user can see. In particular, it does not contain
 * the invisible leading newline of a text document, neither in the UI nor in the model derived from it.
 */
class PlainTextTest extends AbstractTextTest {

  @Test
  void testPlainTextOfTextHasNoLeadingNewline() {
    assertEquals("abc", textArea("abc").getPlainText());
  }

  @Test
  void testPlainTextOfEmptyTextIsEmpty() {
    assertEquals("", textArea("").getPlainText());
  }

  @Test
  void testPlainTextKeepsLineBreaksBetweenParagraphs() {
    assertEquals("a\nb", textArea("<p>a</p><p>b</p>").getPlainText());
  }

  @Test
  void testModelCarriesTheVisiblePlainText() {
    assertEquals("abc", textArea("abc").toModel(true).plainText);
  }

  /**
   * Scenario: A step text is cut by an image into two text areas. The plain text of the step has the
   * text areas on separate lines, but neither a leading newline nor an empty line for the image.
   */
  @Test
  void testPlainTextOfStepPutsTextAreasOnSeparateLines() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textAreaOf(step);
    text.setText("firstsecond");
    text.setCaretPosition("first".length() + 1);

    text.addImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));

    assertEquals("first\nsecond", step.getTextShef().getPlainText());
  }
}
