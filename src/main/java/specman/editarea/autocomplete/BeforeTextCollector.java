package specman.editarea.autocomplete;

import specman.Specman;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import javax.swing.text.JTextComponent;
import java.awt.Container;
import java.util.List;

/** Traverses the hierarchy of {@link EditArea}s, {@link EditContainer}s, {@link AbstractSchrittView}s,
 *  and {@link SchrittSequenzView}s upwards from a given caret position in a {@link TextEditArea} to
 *  collect the plain text preceding the caret, up to {@link #MAX_CHARS} characters.
 * <p>
 * Reading order (innermost first, then upward):
 * <ol>
 *   <li>Text before the caret in the current {@link TextEditArea}</li>
 *   <li>All text areas of preceding {@link EditArea}s in the same {@link EditContainer}.
 *     Recursively downwards in case of sub-structured {@link EditArea}s (tables and list items).</li>
 *   <li>All text areas of preceding {@link EditContainer}s if nested inside a
 *       {@link TableEditArea} or {@link AbstractListItemEditArea}</li>
 *   <li>All steps preceding the current step in its {@link SchrittSequenzView},
 *       recursively descending into nested containers</li>
 *   <li>Enclosing steps and their sequences, ascending to the root sequence</li>
 *   <li>The diagram intro {@link EditContainer}, if the origin {@link TextEditArea} itself is not
 *      located in the intro.</li>
 * </ol>
 * To avoid tangling upward traversal through edit containers / steps / step sequences with downwards traversals
 * through text / table / list item edit areas, the class first collects the edit containers to consider and then
 * performs the text collection in a second step.
 */
class BeforeTextCollector {
  private static final int MAX_CHARS = 5000;

  private final TextEditArea originEditArea;
  private final StringBuilder buffer = new StringBuilder();

  BeforeTextCollector(TextEditArea originEditArea) {
    this.originEditArea = originEditArea;
  }

  String collect(WrappedPosition caretPosition) {
    prependCaretPrecedingText(caretPosition);
    return buffer.toString();
  }

  private boolean limitReached() {
    return buffer.length() >= MAX_CHARS;
  }

  private void prepend(String text) {
    if (!limitReached()) {
      buffer.insert(0, text);
    }
  }

  private void prependCaretPrecedingText(WrappedPosition caretPosition) {
    WrappedDocument doc = originEditArea.getWrappedDocument();
    WrappedPosition start = doc.start();
    int length = caretPosition.distance(start);
    if (length > 0) {
      prepend(doc.getText(start, length));
    }
  }

}
