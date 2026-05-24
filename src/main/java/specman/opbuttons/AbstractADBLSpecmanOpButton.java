package specman.opbuttons;

import specman.*;

import specman.undo.manager.UndoRecording;

/**
 * Base class for toolbar buttons whose operation must be recorded as a single
 * atomic undo step. ADBL is short for <b>A</b>tomic un<b>D</b>oa<b>BL</b>e.
 * Wraps {@link #execute()} in {@link Specman#composeUndo()}.
 */
public abstract class AbstractADBLSpecmanOpButton extends AbstractSpecmanOpButton {

  public AbstractADBLSpecmanOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void registerActionListener() {
    addActionListener(e -> {
      try (UndoRecording ur = composeUndo()) {
        execute();
      }
      catch (EditException ex) {
        context.showError(ex);
      }
    });
  }

}
