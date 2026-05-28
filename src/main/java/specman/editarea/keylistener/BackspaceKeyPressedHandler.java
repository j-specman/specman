package specman.editarea.keylistener;

import specman.EditorI;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedPosition;
import specman.undo.manager.UndoRecording;

import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.event.KeyEvent;

import static specman.editarea.markups.CharType.ParagraphBoundary;
import static specman.Specman.editor;

class BackspaceKeyPressedHandler extends AbstractRemovalKeyPressedHandler {
  BackspaceKeyPressedHandler(TextEditArea textArea, KeyEvent keyEvent) {
    super(textArea, keyEvent);
  }

  void handle() {
    WrappedPosition caretPos = getWrappedCaretPosition();
    if (dissolveEdiArea(textArea, caretPos)) {
      event.consume();
      return;
    }
    if (shouldPreventActionInsideStepnumberLink()) {
      skipToStepnumberLinkEnd();
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
    else if (stepnumberLinkStyleSet(getWrappedSelectionEnd().dec())) {
      removeStepnumberLinkBefore();
      event.consume();
    }
    else if (!caretPos.isZero() && ParagraphBoundary.at(caretPos.dec())) {
      // We are about to merge two paragraphs, so must ensure markup recovery
      backupMarkupsAndRecoverAfterDefaultKeyOperation();
    }
  }

  /** This compensates a nasty glitch in JEditorPane: usually the underlying document
   * ends with an invisible newline. However, when the user types RETURN at the end of
   * the text, this causes a new empty line to appear in the UI without any change in
   * the document. The ending newline is now visible, so to say, and the use can not
   * get rid of it by default. So we check for this situation here and cleen up the
   * text by re-resetting the complete content which causes thr UI to be corrected. */
  private boolean removeTrailingEmptyLine() {
    WrappedPosition caretPositon = getWrappedCaretPosition();
    WrappedPosition startOffset = getWrappedSelectionStart();
    WrappedPosition endOffset = getWrappedSelectionEnd();

    if (endOffset.equals(startOffset) && caretPositon.isLast() && !caretPositon.isZero()) {
      try {
        int rowStart = Utilities.getRowStart(textArea, textArea.getCaretPosition());
        int rowEnd = Utilities.getRowEnd(textArea, textArea.getCaretPosition());
        if (rowStart == rowEnd) {
          try(UndoRecording ur = editor().composeUndo()) {
            cleanupText();
          }
          return true;
        }
      }
      catch(BadLocationException blx) {
        throw new RuntimeException(blx);
      }
    }
    return false;
  }

  /** When pressing BACKSPACE at the very beginning of a text edit area we check if this
   * will cause the area to be dissolved by merging its content to other areas. However,
   * we do that only of there is no text selected at this moment. A text selection will
   * cause the ordinary handling to take place. */
  private boolean dissolveEdiArea(TextEditArea textArea, WrappedPosition caretPos) {
    if (caretPos.isZero()) {
      if (textArea.getSelectionStart() == textArea.getSelectionEnd()) {
        return textArea.dissolveEditArea() != null;
      }
    }
    return false;
  }

  public void removeStepnumberLinkBefore() {
    EditorI editor = editor();
    try (UndoRecording ur = editor.composeUndo()) {
      WrappedPosition position = getWrappedSelectionEnd().dec();
      WrappedPosition startOffset = getWrappedSelectionStart().min(getStartOffsetFromPosition(position));
      WrappedPosition endOffset = getEndOffsetFromPosition(position);
      removeTextAndUnregisterStepnumberLinks(startOffset, endOffset);
    }
  }

  protected void handleTextDeletion() {
    if (getSelectionStart() == getSelectionEnd()) {
      handleTextDeletion(getSelectionStart() - 1, getSelectionEnd());
    } else {
      handleTextDeletion(getSelectionStart(), getSelectionEnd());
    }
  }


}
