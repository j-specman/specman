package specman.editarea.keylistener;

import org.apache.commons.lang.StringUtils;
import specman.EditException;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.TextStyles;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.suggest.github.CopilotClient;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import java.awt.event.KeyEvent;

public class TabKeyPressedHandler extends AbstractKeyEventHandler {
  public TabKeyPressedHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea, event);
  }

  @Override
  void handle() {
    try {
      if (autoCompletionPending()) {
        commitAutoCompletion();
      } else if (isEditable()) {
        addPendingAutocompletion();
      }
      event.consume();
    }
    catch (Exception e) {
      Specman.instance().showError(new EditException(e.getMessage()));
    }
  }

  private boolean autoCompletionPending() {
    return textArea().getPendingAutoCompletionEnd() != null;
  }

  private void commitAutoCompletion() {
    WrappedDocument doc = getWrappedDocument();
    WrappedPosition caretPosition = getWrappedCaretPosition();
    WrappedPosition autoCompletionEnd = textArea().getPendingAutoCompletionEnd();
    AttributeSet attributeSet = doc.getCharacterElement(caretPosition.dec()).getAttributes();
    int completionLength = autoCompletionEnd.distance(caretPosition);
    String completion = doc.getText(caretPosition, completionLength);
    try (UndoRecording ur = Specman.instance().pauseUndo()) {
      doc.remove(caretPosition, completionLength);
    }
    doc.insertString(caretPosition, completion, attributeSet);
    textArea().setPendingAutoCompletionEnd(null);
  }

  private void addPendingAutocompletion() throws Exception {
    String completion = suggestCompletion();
    if (!StringUtils.isEmpty(completion)) {
      WrappedDocument doc = getWrappedDocument();
      WrappedPosition caretPosition = getWrappedCaretPosition();
      AttributeSet previousAttribute = doc.getCharacterElement(caretPosition).getAttributes();
      MutableAttributeSet suggestAttributes = new SimpleAttributeSet(previousAttribute);
      suggestAttributes.addAttributes(TextStyles.autoSuggestStyle);
      try (UndoRecording ur = Specman.instance().pauseUndo()) {
        doc.insertString(caretPosition, completion, suggestAttributes);
        setCaretPosition(caretPosition.unwrap());
      }
      textArea().setPendingAutoCompletionEnd(caretPosition.inc(completion.length()));
    }
  }

  private String suggestCompletion() throws Exception{
    WrappedDocument doc = getWrappedDocument();
    WrappedPosition caretPosition = getWrappedCaretPosition();
    WrappedPosition start = doc.start();
    WrappedPosition end = doc.end();
    String before = doc.getText(start, caretPosition.distance(start));
    String after = doc.getText(caretPosition, end.distance(caretPosition));
    String completion = new CopilotClient().complete(before, after);
    return completion;
  }
}
