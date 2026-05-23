package specman.editarea.keylistener;

import specman.EditorI;
import specman.Specman;
import specman.editarea.StepnumberLink;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;
import specman.undo.UndoableStepnumberLinkRemoved;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;

import java.awt.event.KeyEvent;

import static specman.graphics.Styles.deletedStepnumberLinkStyle;
import static specman.graphics.Styles.geloeschtStil;

abstract class AbstractRemovalKeyPressedHandler extends AbstractKeyEventHandler {
  protected AbstractRemovalKeyPressedHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea, event);
  }

  /**
   * This handles the deletion/mark as deletion of text. <p>
   * When looping backwards through the selected text, there's no need to worry
   * about the next text position being moved because of a deletion.
   * <p>
   * When deleting a range there can be the following items in it: <p>
   * - Normal text - White Background - Gets marked as deleted <p>
   * - Changed text - Yellow Background - Gets deleted <p>
   * - Marked as deleted text - Yellow Background with strikethrough - No changes
   */
  protected WrappedPosition handleTextDeletion(int pStartOffset, int pEndOffset) {
    WrappedPosition startOffset = getWrappedDocument().fromUI(pStartOffset);
    WrappedPosition endOffset = getWrappedDocument().fromUI(pEndOffset);
    WrappedPosition maxDeletionMarked = endOffset.copy();

    if (pStartOffset <= 0) {
      setCaretPosition(1);
      return maxDeletionMarked;
    }

    EditorI editor = Specman.instance();

    try (UndoRecording ur = editor.composeUndo()) {
      for (WrappedPosition currentEndPosition = endOffset; currentEndPosition.greater(startOffset); ) { // The missing position-- is intended, see below
        WrappedElement element = getWrappedDocument().getCharacterElement(currentEndPosition.dec()); // -1 since we look at the previous character
        WrappedPosition linkStilStart = element.getStartOffset();
        WrappedPosition linkStilEnd = element.getEndOffset();
        WrappedPosition currentStartPosition = startOffset.max(linkStilStart);
        int length = currentEndPosition.distance(currentStartPosition);

        if (length < 1) {
          throw new RuntimeException("Deletion length <= 1. There seems to be a bug in this method().");
        }

        if (elementIsChangedButNotMarkedAsDeleted(element)) {
          if (stepnumberLinkChangedStyleSet(element)) {
            removeTextAndUnregisterStepnumberLinks(linkStilStart, linkStilEnd, editor);
          }
          else {
            removeTextAndUnregisterStepnumberLinks(currentStartPosition, currentEndPosition, editor);
          }
        }
        else {
          if (elementHatDurchgestrichenenText(element)) { // No need to reapply deletedStyle if it's already set
            if (stepnumberLinkChangedStyleSet(currentStartPosition)) {
              setCaretPosition(linkStilStart.unwrap());
            }
            else {
              setCaretPosition(currentStartPosition.unwrap());
            }
          }
          else if (stepnumberLinkNormalStyleSet(currentStartPosition)) {
            markRangeAsDeleted(linkStilStart, linkStilEnd.distance(linkStilStart), deletedStepnumberLinkStyle);
            maxDeletionMarked = maxDeletionMarked.max(linkStilEnd);
            setCaretPosition(linkStilStart.unwrap());
          }
          else {
            markRangeAsDeleted(currentStartPosition, length, geloeschtStil);
            setCaretPosition(currentStartPosition.unwrap());
          }
        }

        currentEndPosition = currentEndPosition.dec(length); // Skip already processed positions
      }
    }

    return maxDeletionMarked;
  }

  protected void removeTextAndUnregisterStepnumberLinks(WrappedPosition startOffset, WrappedPosition endOffset, EditorI editor) {
    if (startOffset.greater(endOffset)) {
      throw new IllegalArgumentException("StartOffSet is greater than EndOffset - Make sure not to set the length as endOffset");
    }

    WrappedDocument doc = getWrappedDocument();

    for (WrappedPosition currentOffset = startOffset; currentOffset.less(endOffset); ) { // The missing currentOffset++ is intended
      WrappedPosition currentEndOffset = getEndOffsetFromPosition(currentOffset);
      int length = currentEndOffset.distance(currentOffset);
      WrappedElement element = doc.getCharacterElement(currentOffset);

      if (stepnumberLinkStyleSet(element)) {
        String stepnumberLinkID = getStepnumberLinkIDFromElement(currentOffset, currentEndOffset);
        if (!StepnumberLink.isStepnumberLinkDefect(stepnumberLinkID)) {
          AbstractSchrittView step = editor.findStepByStepID(stepnumberLinkID);
          step.unregisterStepnumberLink(textArea);
          editor.addEdit(new UndoableStepnumberLinkRemoved(step, textArea));
        }
      }

      currentOffset = currentOffset.inc(length); // Skip already processed positions
    }

    doc.remove(startOffset, endOffset.distance(startOffset));
  }

  private boolean stepnumberLinkChangedStyleSet(WrappedPosition position) {
    WrappedDocument doc = getWrappedDocument();
    return stepnumberLinkChangedStyleSet(doc.getCharacterElement(position));
  }

  private boolean elementIsChangedButNotMarkedAsDeleted(WrappedElement element) {
    return (elementHatAenderungshintergrund(element) || stepnumberLinkChangedStyleSet(element))
      && !elementHatDurchgestrichenenText(element);
  }

}
