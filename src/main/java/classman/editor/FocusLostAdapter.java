package classman.editor;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Runs something when a component loses the focus. */
class FocusLostAdapter extends FocusAdapter {
  private final Runnable focusLost;

  FocusLostAdapter(Runnable focusLost) {
    this.focusLost = focusLost;
  }

  @Override
  public void focusLost(FocusEvent e) {
    focusLost.run();
  }
}
