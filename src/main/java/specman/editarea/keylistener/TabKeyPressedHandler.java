package specman.editarea.keylistener;

import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;

import java.awt.event.KeyEvent;

public class TabKeyPressedHandler extends AbstractKeyEventHandler {
  public TabKeyPressedHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea, event);
  }

  @Override
  void handle() {
    // Control + Tab is Swing's default focus traversal key, so we only want to handle Tab key presses without Control modifier.
    if (!event.isControlDown()) {
      EditContainer editContainer = textArea.getParent();
      TableEditArea table = editContainer.getContainer(TableEditArea.class);
      if (table != null) {
        table.focusNextCell(editContainer);
        event.consume();
      }
    }
  }

}
