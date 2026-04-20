package specman.editarea.keylistener;

import specman.editarea.TextEditArea;

import java.awt.event.KeyEvent;

public class EscapeKeyPressedHandler extends AbstractKeyEventHandler {
  public EscapeKeyPressedHandler(TextEditArea textArea, KeyEvent keyEvent) {
    super(textArea, keyEvent);
  }

  public void handle() {
    // Actually this is not really needed as any other key than TAB will cause the pending autocompletion to be reset.
    // However, the {@link TextEditArea} triggers an ESC key press event when loosing focus. So only for this case we
    // keep the line here.
    resetSuggestedAutoCompletion();
  }
}
