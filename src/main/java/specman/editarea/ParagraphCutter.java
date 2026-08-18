package specman.editarea;

import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;

/** This class splits a TextEdiArea into multiple areas based on the current caret position.
 * In the maximum case, the caret position is placed in a paragraph with leading and trailing
 * paragraphs within the same text area. As a result, the cutter produces
 * <ol>
 *   <li>a new TextEditArea containing only the text of the paragraph which the caret was in, and</li>
 *   <li>a new TextEditArea containing the text of all trailing paragraphs, and</li>
 *   <li>a limitation of the initiating TextEditArea to the text from all leading paragraphs.</li>
 * </ol>
 * The two new TextEditAreas have to be added to the parent EditContainer of the initiating TextEditArea.
 * This is more complicated than it sounds, as there are different cases to consider (e.g. a completely
 * empty area as an extreme) and different representations of paragraphs separations in a StyledDocument.
 */
public class ParagraphCutter {
  private TextEditArea caretArea;
  private TextEditArea trailingArea;
  private WrappedDocument initiatingDocument;
  private WrappedPosition caretPosition;

  public ParagraphCutter cutAtCaret(TextEditArea initiatingArea) {
    this.caretPosition = initiatingArea.getWrappedCaretPosition();
    this.initiatingDocument = initiatingArea.getWrappedDocument();
    WrappedPosition leadingAreaStart = initiatingDocument.start();
    WrappedPosition caretAreaStart = findCaretAreaStart();
    WrappedPosition caretAreaEnd = findCaretAreaEnd();
    WrappedPosition leadingAreaEnd = findLeadingAreaEnd();
    WrappedPosition trailingAreaStart = findTrailingAreaStart(caretAreaEnd);

    caretArea = initiatingArea.copySection(caretAreaStart, caretAreaEnd);
    if (caretArea == null) {
      caretArea = new TextEditArea(initiatingArea.getFont());
    }
    trailingArea = initiatingArea.copySection(trailingAreaStart, initiatingDocument.end());
    initiatingArea.shrink(leadingAreaStart, leadingAreaEnd);

    return this;
  }

  private WrappedPosition findTrailingAreaStart(WrappedPosition caretAreaEnd) {
    return caretAreaEnd.inc().min(initiatingDocument.end().inc());
  }

  private WrappedPosition findLeadingAreaEnd() {
    return findCaretAreaStart().dec();
  }

  private WrappedPosition findCaretAreaEnd() {
    return currentParagraphElement().getEndOffset().dec();
  }

  private WrappedPosition findCaretAreaStart() {
    return currentParagraphElement().getStartOffset();
  }

  private WrappedElement currentParagraphElement() {
    return initiatingDocument.getParagraphElement(caretPosition);
  }

  public TextEditArea caretTextArea() {
    return caretArea;
  }

  public EditArea trailingTextArea() {
    return trailingArea;
  }
}
