package specman.editarea.keylistener;

import specman.EditException;
import specman.EditorI;
import specman.Specman;
import specman.editarea.autocomplete.AutoCompletion;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.suggest.github.CopilotAuth;
import specman.undo.manager.UndoRecording;

import javax.swing.text.AttributeSet;
import java.awt.event.KeyEvent;

public class TabKeyPressedHandler extends AbstractKeyEventHandler {
  public TabKeyPressedHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea, event);
  }

  @Override
  void handle() {
    EditorI specman = Specman.instance();
    try {
      if (autoCompletionInitiated()) {
        // Don't do anything if an auto completion is already in work but no suggestion is present yet.
        // That means: we are waiting for a result from a GitHub server call and don't want initiate
        // another one.
      }
      else if (autoCompletionPending()) {
        commitAutoCompletion();
      }
      else if (isEditable() && specman.autocompleteOn()) {
        initiateAutoCompletion();
      }
      event.consume();
    }
    catch (Exception e) {
      specman.showError(new EditException(e.getMessage()));
    }
  }

  private boolean autoCompletionInitiated() {
    AutoCompletion completion = textArea().getAutoCompletion();
    return completion != null && !completion.suggestionPresent();
  }

  private boolean autoCompletionPending() {
    AutoCompletion completion = textArea().getAutoCompletion();
    return completion != null && completion.suggestionPresent();
  }

  private void commitAutoCompletion() {
    AutoCompletion completion = textArea().getAutoCompletion();
    WrappedDocument doc = getWrappedDocument();
    WrappedPosition caretPosition = getWrappedCaretPosition();
    WrappedPosition suggestionEnd = completion.getSuggestedCompletionEnd();
    int suggestionLength = suggestionEnd.distance(caretPosition);
    String suggestion = doc.getText(caretPosition, suggestionLength);
    try (UndoRecording ur = Specman.instance().pauseUndo()) {
      doc.remove(caretPosition, suggestionLength);
    }
    AttributeSet attributeSet = prepareCorrectStyleForTextInput();
    doc.insertString(caretPosition, suggestion, attributeSet);
    textArea().setAutoCompletion(null);
  }

  /** Requesting a suggestion for an auto completion must be initiated asynchronously
   * as it might require and interactive renewal of an API token (see class {@link CopilotAuth}).
   * Therefore we must not block the event loop of the initiating main thread. */
  private void initiateAutoCompletion() {
    AutoCompletion autoCompletion = new AutoCompletion(textArea());
    textArea().setAutoCompletion(autoCompletion);
    new Thread(autoCompletion).start();
  }

}
