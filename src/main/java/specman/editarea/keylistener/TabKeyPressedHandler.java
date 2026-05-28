package specman.editarea.keylistener;

import specman.Specman;
import specman.editarea.TextEditArea;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TabKeyPressedHandler extends AbstractKeyEventHandler {
  public TabKeyPressedHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea, event);
  }

  @Override
  void handle() {
    if (event.isControlDown()) {
      // When CTRL is pressed, the TAB key causes the focus to be moved from one text area to the next.
      // But the scroll position may require to be changed to make the new focussed text area visible.

      // Das hier klappt noch nicht: Wenn man CTRL drückt, dann kommt das TAB-Drücken hier nicht an

      //SwingUtilities.invokeLater(() -> editor().scrollTo(800));
    }
  }
}
