package specman.editarea.autocomplete;

import org.apache.commons.lang.StringUtils;
import specman.EditException;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.TextStyles;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.editarea.keylistener.AbstractKeyHandler;
import specman.editarea.keylistener.EscapeKeyPressedHandler;
import specman.editarea.keylistener.TabKeyPressedHandler;
import specman.suggest.github.CopilotClient;
import specman.undo.manager.UndoRecording;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/** This class represents a running auto-completion based on a suggestions from GitHub Copilot completion service.
 * The service request and a potentially required OAuth handshake for a new API token is performed asynchronusly
 * in the run method. The result is the suggested string, directly pasted into the text field at the current caret
 * position with a special font style. The suggested completion can be accepted by the user by pressing the TAB
 * key (see class {@link TabKeyPressedHandler} or denied by pressing ESC or any other key than TAB or by moving the
 * focus away from the text field (see class {@link EscapeKeyPressedHandler} resp. {@link AbstractKeyHandler#resetSuggestedAutoCompletion()}). */
public class AutoCompletion implements Runnable {
  private final TextEditArea editArea;
  private final WrappedDocument doc;
  private WrappedPosition suggestedCompletionEnd;
  private boolean stopped = false;

  public AutoCompletion(TextEditArea textEditArea) {
    this.editArea = textEditArea;
    this.doc = textEditArea.getWrappedDocument();
  }

  public void run() {
    try {
      String completion = requestCompletionSuggestion();
      if (!StringUtils.isEmpty(completion) && !stopped) {
        addPendingCompletion(completion);
      }
    }
    catch(EditException ex) {
      ex.printStackTrace();
      Specman.instance().showError(ex);
    }
    catch (Exception x) {
      x.printStackTrace();
      Specman.instance().showError(new EditException("Auto completion failed" + x.getMessage()));
    }
  }

  private String requestCompletionSuggestion() throws Exception {
    WrappedPosition caretPosition = editArea.getWrappedCaretPosition();
    String before = assembleBeforeText(caretPosition);
    String after = assembleAfterText(caretPosition);
    return new CopilotClient().complete(before, after);
  }

  private String assembleBeforeText(WrappedPosition caretPosition) {
    return new BeforeTextCollector(editArea).collect(caretPosition);
  }

  private String assembleAfterText(WrappedPosition caretPosition) {
    WrappedPosition end = doc.end();
    return doc.getText(caretPosition, end.distance(caretPosition));
  }

  private void addPendingCompletion(String completion) {
    WrappedDocument doc = editArea.getWrappedDocument();
    WrappedPosition caretPosition = editArea.getWrappedCaretPosition();
    AttributeSet previousAttribute = doc.getCharacterElement(caretPosition).getAttributes();
    MutableAttributeSet suggestAttributes = new SimpleAttributeSet(previousAttribute);
    suggestAttributes.addAttributes(TextStyles.autoSuggestStyle);
    try (UndoRecording ur = Specman.instance().pauseUndo()) {
      doc.insertString(caretPosition, completion, suggestAttributes);
      editArea.setCaretPosition(caretPosition.unwrap());
      suggestedCompletionEnd = caretPosition.inc(completion.length());
    }
  }

  public boolean suggestionPresent() {
    return suggestedCompletionEnd != null;
  }

  public WrappedPosition getSuggestedCompletionEnd() {
    return suggestedCompletionEnd;
  }

  public WrappedPosition stop() {
    stopped = true;
    return getSuggestedCompletionEnd();
  }
}
