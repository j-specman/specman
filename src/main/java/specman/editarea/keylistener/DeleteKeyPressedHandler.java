package specman.editarea.keylistener;

import specman.EditorI;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedPosition;
import specman.undo.manager.UndoRecording;

import java.awt.event.KeyEvent;

import static specman.editarea.markups.CharType.ParagraphBoundary;
import static specman.Specman.editor;

class DeleteKeyPressedHandler extends AbstractRemovalKeyPressedHandler {
  DeleteKeyPressedHandler(TextEditArea textArea, KeyEvent keyEvent) {
    super(textArea, keyEvent);
  }

  void handle() {
    WrappedPosition caretPos = getWrappedCaretPosition();
    if (shouldPreventActionInsideStepnumberLink()) {
      skipToStepnumberLinkStart();
      event.consume();
      return;
    }
    if (removeTrailingEmptyLine()) {
      event.consume();
      return;
    }
    if (isTrackingChanges()) {
      handleTextDeletion();
      event.consume();
    }
    else if (stepnumberLinkStyleSetAt(getWrappedSelectionStart())) {
      removeStepnumberLinkAfter();
      event.consume();
    }
    else if (ParagraphBoundary.at(caretPos)) {
      // We are about to merge two paragraphs, so must ensure markup recovery
      backupMarkupsAndRecoverAfterDefaultKeyOperation();
    }
  }

  /** Symmetric to BackspaceKeyPressedHandler#removeTrailingEmptyLine: when the caret
   * stands at the end of the second-to-last line (character at caret is a paragraph
   * boundary and the next position is the last), Delete would try to enter the trailing
   * empty line rather than remove it. cleanupText() fixes the UI instead. */
  private boolean removeTrailingEmptyLine() {
    WrappedPosition caretPos = getWrappedCaretPosition();
    if (getWrappedSelectionStart().equals(getWrappedSelectionEnd())
        && ParagraphBoundary.at(caretPos)
        && caretPos.inc().isLast()) {
      try (UndoRecording ur = editor().composeUndo()) {
        cleanupText();
      }
      return true;
    }
    return false;
  }

  void removeStepnumberLinkAfter() {
    EditorI editor = editor();
    try (UndoRecording ur = editor.composeUndo()) {
      WrappedPosition position = getWrappedSelectionStart().inc();
      WrappedPosition endOffset = getWrappedSelectionEnd().max(getEndOffsetFromPosition(position));
      WrappedPosition startOffset = getStartOffsetFromPosition(position);
      removeTextAndUnregisterStepnumberLinks(startOffset, endOffset);
    }
  }

  protected void handleTextDeletion() {
    int deleteTo = (getSelectionStart() == getSelectionEnd())
      ? getSelectionEnd() + 1
      : getSelectionEnd();
    int lengthBefore = getWrappedDocument().getLength();
    WrappedPosition maxDeleteMark = handleTextDeletion(getSelectionStart(), deleteTo);

    // Characters added in change mode are removed for real, which moves the following text to the left.
    // Only characters marked as deleted stay and the caret has to move behind them.
    int removedLength = lengthBefore - getWrappedDocument().getLength();
    maxDeleteMark = maxDeleteMark.dec(removedLength);

    // If the deleted text reaches up to the very end of the text area's content,
    // we can't move the caret beyond that position.
    if (!maxDeleteMark.exists()) {
      maxDeleteMark = getWrappedDocument().end();
    }
    setCaretPosition(maxDeleteMark.unwrap());
  }

}
