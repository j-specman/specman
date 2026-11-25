package specman;

import net.atlanticbb.tantlinger.ui.text.actions.HTMLAlignAction;
import specman.editarea.TextEditArea;

import javax.swing.AbstractButton;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Gives tests access to the alignment buttons of the toolbar, which are only visible within this package. */
final class AlignmentButtonsSupport {

  private AlignmentButtonsSupport() {
  }

  /** A focus event gives SHEF its editor and lets it set the state of the buttons. */
  static TextEditArea focus(TextEditArea area) {
    area.setCaretPosition(1);
    for (FocusListener listener : area.getFocusListeners()) {
      listener.focusGained(new FocusEvent(area, FocusEvent.FOCUS_GAINED));
    }
    specman.setLastFocusedTextArea(area);
    return area;
  }

  /** The buttons in the order of {@link HTMLAlignAction#LEFT} to {@link HTMLAlignAction#JUSTIFY}. */
  static List<AbstractButton> alignButtons() {
    List<AbstractButton> buttons = ((Specman) specman).shefEditorPane.getTextGroupComponents().stream()
      .filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
      .filter(button -> button.getAction() instanceof HTMLAlignAction).toList();
    assertEquals(4, buttons.size());
    return buttons;
  }

  static void click(int alignment) {
    alignButtons().get(alignment).doClick();
  }
}
