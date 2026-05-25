package specman.ops;

import specman.EditException;
import specman.SpecmanOpContext;
import specman.opbuttons.AbstractADBLSpecmanOpButton;
import specman.undo.manager.UndoRecording;

/**
 * Base class for Specman operations that must be recorded as a single atomic undoable
 * operation. ADBL is short for <b>A</b>tomic un<b>D</b>oa<b>BL</b>e.
 * Wraps {@link #execute()} in {@link specman.Specman#composeUndo()} and handles {@link EditException}.
 * Parallel to {@link AbstractADBLSpecmanOpButton} for the non-button op hierarchy.
 */
public abstract class AbstractADBLSpecmanOp extends AbstractSpecmanOp {

  protected AbstractADBLSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void run() {
    try (UndoRecording ur = composeUndo()) {
      execute();
    }
    catch (EditException ex) {
      context.showError(ex);
    }
  }

  abstract void execute() throws EditException;

}
