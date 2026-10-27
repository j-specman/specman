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
    Optional<EditArea> target = new EditAreaNeighbourFinder(workingArea).find(currentFocusArea, desiredX, direction);
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

  /** Puts the caret in the text's first or last row at the given x position, as close as the row's text allows. */
  private static void placeCaret(TextEditArea text, int x, boolean inFirstRow) {
    try {
      int rowOffset = inFirstRow ? VISIBLE_TEXT_START : text.getDocument().getLength();
      Rectangle2D row = text.modelToView2D(rowOffset);
      Point wanted = new Point(x, (int) row.getCenterY());
      text.setCaretPosition(text.viewToModel2D(wanted));
      // Setting the caret position resets Swing's memory of the column. It is what lets the next arrow key press
      // in this text, or the next jump, continue at the wanted x position although this row may be shorter.
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
