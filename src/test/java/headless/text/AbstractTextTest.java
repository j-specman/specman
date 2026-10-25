package headless.text;

import headless.HeadlessExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.TextEditArea;
import specman.model.v002.TextEditAreaModel_V002;
import specman.view.AbstractSchrittView;

import java.awt.Font;

/** Base class for tests about the text of edit areas. It provides the headless Specman and the creation of text areas. */
@ExtendWith(HeadlessExtension.class)
abstract class AbstractTextTest {

  private static final Font FONT = new Font("Dialog", Font.PLAIN, 12);

  /** A text area with the given HTML as content, which doesn't belong to any step. */
  static TextEditArea textArea(String html) {
    return new TextEditArea(new TextEditAreaModel_V002(html), FONT);
  }

  /** The first text area of the given step, which is the one with the step's text. */
  static TextEditArea textAreaOf(AbstractSchrittView step) {
    return step.getFirstEditArea().asTextArea();
  }
}
