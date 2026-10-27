package specman.editarea.focusmover;

import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedBadLocationException;

import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.Point;

import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

public class CrossEditAreaFocusMoverFromText extends AbstractCrossEditAreaFocusMover<TextEditArea> {

  public CrossEditAreaFocusMoverFromText(TextEditArea currentFocusArea) {
    super(currentFocusArea);
  }

  @Override
  protected boolean caretAtBottom() {
    try {
      int lastOffset = currentFocusArea.getDocument().getLength();
      return Utilities.getRowEnd(currentFocusArea, currentFocusArea.getCaretPosition())
        == Utilities.getRowEnd(currentFocusArea, lastOffset);
    }
    catch (BadLocationException blx) {
      throw new WrappedBadLocationException(blx);
    }
  }

  @Override
  protected boolean caretAtTop() {
    try {
      return Utilities.getRowStart(currentFocusArea, currentFocusArea.getCaretPosition())
        == Utilities.getRowStart(currentFocusArea, VISIBLE_TEXT_START);
    }
    catch (BadLocationException blx) {
      throw new WrappedBadLocationException(blx);
    }
  }

  /**
   * Swing remembers the column the caret came from, so that moving through a short row doesn't lose it.
   * That's the x position we want to keep, falling back to the caret's own one.
   */
  @Override
  protected int desiredLocalX() {
    Point magicCaretPosition = currentFocusArea.getCaret().getMagicCaretPosition();
    if (magicCaretPosition != null) {
      return magicCaretPosition.x;
    }
    try {
      return (int) currentFocusArea.modelToView2D(currentFocusArea.getCaretPosition()).getX();
    }
    catch (BadLocationException blx) {
      throw new WrappedBadLocationException(blx);
    }
  }
}
