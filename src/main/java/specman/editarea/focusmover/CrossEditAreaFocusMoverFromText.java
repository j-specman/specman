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
   * The column the caret wants to be in, which is not necessarily where it currently stands, see the
   * <i>magic caret position</i> in the {@link AbstractCrossEditAreaFocusMover class comment}. Only if Swing
   * doesn't know one yet, the caret's actual x position is used.
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
