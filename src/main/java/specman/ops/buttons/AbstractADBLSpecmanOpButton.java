package specman.ops.buttons;

import specman.*;

import specman.ops.SpecmanOpContext;
import specman.undo.manager.UndoRecording;

/**
 * Base class for toolbar buttons whose operation must be recorded as a single
 * atomic undo step. ADBL is short for <b>A</b>tomic un<b>D</b>oa<b>BL</b>e.
 * Wraps {@link #execute()} in {@link Specman#composeUndo()}.
 */
public abstract class AbstractADBLSpecmanOpButton extends AbstractSpecmanOpButton {

  public AbstractADBLSpecmanOpButton(SpecmanOpContext context) {
    super(context);
  }

  @Override
  void registerActionListener() {
    addActionListener(e -> {
      if (!confirmed()) {
        return;
      }
      try (UndoRecording ur = composeUndo()) {
        execute();
      }
      catch (EditException ex) {
        context.showError(ex);
      }
      finally {
        finished();
      }
    });
  }

  /** Called before the operation starts, to let the user cancel it.
   * @return false if the operation must not be executed */
  boolean confirmed() {
    return true;
  }

  /** Called after an operation which {@link #confirmed()} let start, whether it succeeded or not. */
  void finished() {
  }

}
