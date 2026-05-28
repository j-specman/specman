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
    if (isTrackingChanges()) {
      handleTextDeletion();
      event.consume();
    }
    else if (stepnumberLinkStyleSet(getWrappedSelectionStart())) {
      removeStepnumberLinkAfter();
      event.consume();
    }
    else if (ParagraphBoundary.at(caretPos)) {
      // We are about to merge two paragraphs, so must ensure markup recovery
      backupMarkupsAndRecoverAfterDefaultKeyOperation();
    }
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
    WrappedPosition maxDeleteMark = handleTextDeletion(getSelectionStart(), deleteTo);

    // If the deleted text reaches up to the very end of the text area's content,
    // we can't move the caret beyond that position.
    if (!maxDeleteMark.exists()) {
      maxDeleteMark = getWrappedDocument().end();
    }
    setCaretPosition(maxDeleteMark.unwrap());
  }

}
