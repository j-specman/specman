package specman.editarea.keylistener;

import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.undo.manager.UndoRecording;

import java.awt.event.KeyEvent;

public class EscapeKeyPressedHandler extends AbstractKeyEventHandler {
  public EscapeKeyPressedHandler(TextEditArea textArea, KeyEvent keyEvent) {
    super(textArea, keyEvent);
  }

  public void handle() {
    WrappedPosition autoCompletionEnd = textArea().getPendingAutoCompletionEnd();
    if (autoCompletionEnd != null) {
      WrappedDocument doc = getWrappedDocument();
      WrappedPosition caretPosition = getWrappedCaretPosition();
      int completionLength = autoCompletionEnd.distance(caretPosition);
      try(UndoRecording ur = Specman.instance().pauseUndo()) {
        doc.remove(caretPosition, completionLength);
      }
      textArea().setPendingAutoCompletionEnd(null);
    }
  }
}
