package specman.editarea.focusmover;

import specman.editarea.EditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedBadLocationException;
import specman.editarea.focusmover.EditAreaNeighbourFinder.Direction;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

import static specman.Specman.editor;
import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

/**
 * Moves the focus with the arrow keys up and down from the current edit area to the graphically next one,
 * which can be anywhere in the diagram: in the same step, in another step or in a table cell. The caret
 * lands as close as possible to the x position it came from.
 * <p>
 * <b>Keeping the column over several moves.</b> A row may be shorter than the column the user is aiming at,
 * e.g. an empty text between two long ones. If the caret simply stood at the end of that short row, the next
 * move would continue from there and the original column would be lost. Text editors avoid this by remembering
 * the column separately from the caret position, and so does Swing: Its <i>magic caret position</i>
 * ({@link javax.swing.text.Caret#getMagicCaretPosition()}) is the point the caret <i>wants</i> to be at. Swing's own
 * up and down actions read it and put it back after moving the caret. Any other caret movement (typing, left and
 * right arrow, clicking) replaces it by the caret's actual position, which is exactly when the user gives up the old
 * column.
 * <p>
 * We use it for the moves across edit areas, too: Before leaving a text, the desired x position is read from its
 * magic caret position, see {@link CrossEditAreaFocusMoverFromText#desiredLocalX()}. After entering the target text,
 * its magic caret position is set to that wanted point, even if the caret itself had to stop earlier,
 * see {@link #placeCaret(TextEditArea, int, boolean)}. Images have no caret, they remember the x position
 * themselves, see {@link ImageEditArea#setNavigationX(int)}.
 */
abstract class AbstractCrossEditAreaFocusMover<EDITAREATYPE extends EditArea> {
  protected final EDITAREATYPE currentFocusArea;

  public AbstractCrossEditAreaFocusMover(EDITAREATYPE currentFocusArea) {
    this.currentFocusArea = currentFocusArea;
  }

  /** @return true if the focus has been moved. False if the caret isn't in the last row, or if there's nothing below. */
  public boolean moveFocusToSucceedingEditArea() {
    return caretAtBottom() && moveFocusTo(Direction.DOWN);
  }

  /** @return true if the focus has been moved. False if the caret isn't in the first row, or if there's nothing above. */
  public boolean moveFocusToPreceedingEditArea() {
    return caretAtTop() && moveFocusTo(Direction.UP);
  }

  private boolean moveFocusTo(Direction direction) {
    Container workingArea = editor().getArbeitsbereich();
    int desiredX = SwingUtilities.convertPoint(currentFocusArea.asComponent(), desiredLocalX(), 0, workingArea).x;
    Optional<EditArea> target = new EditAreaNeighbourFinder(editor()).find(currentFocusArea, desiredX, direction);
    target.ifPresent(area -> enter(area, desiredX, direction, workingArea));
    return target.isPresent();
  }

  private void enter(EditArea target, int desiredX, Direction direction, Container workingArea) {
    int localX = SwingUtilities.convertPoint(workingArea, desiredX, 0, target.asComponent()).x;
    if (target.isTextArea()) {
      placeCaret(target.asTextArea(), localX, direction == Direction.DOWN);
    }
    else {
      ImageEditArea image = target.asImageArea();
      image.setNavigationX(localX);
      image.scrollRectToVisible(new Rectangle(image.getWidth(), image.getHeight()));
    }
    editor().focus(target);
  }

  /**
   * Puts the caret in the text's first or last row at the given x position, as close as the row's text allows.
   * The caret may end up left of the wanted x position if the row is shorter, so the wanted point is also stored as
   * the text's magic caret position, see the class comment.
   */
  private static void placeCaret(TextEditArea text, int x, boolean inFirstRow) {
    try {
      int rowOffset = inFirstRow ? VISIBLE_TEXT_START : text.getDocument().getLength();
      Rectangle2D row = text.modelToView2D(rowOffset);
      Point wanted = new Point(x, (int) row.getCenterY());
      text.setCaretPosition(text.viewToModel2D(wanted));
      // Must come after setCaretPosition(), which overwrites the magic caret position with the caret's actual one.
      text.getCaret().setMagicCaretPosition(wanted);
      text.scrollRectToVisible(text.modelToView2D(text.getCaretPosition()).getBounds());
    }
    catch (BadLocationException blx) {
      throw new WrappedBadLocationException(blx);
    }
  }

  /** The x position to continue from, in the coordinates of the current focus area. */
  abstract protected int desiredLocalX();

  abstract protected boolean caretAtTop();

  abstract protected boolean caretAtBottom();

}
