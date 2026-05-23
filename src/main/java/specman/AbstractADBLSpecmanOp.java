package specman;

import specman.undo.manager.UndoRecording;

/**
 * Base class for Specman operations that must be recorded as a single atomic undoable
 * operation. ADBL is short for <b>A</b>tomic un<b>D</b>oa<b>BL</b>e.
 * Wraps {@link #execute()} in {@link Specman#composeUndo()} and handles {@link EditException}.
 * Parallel to {@link AbstractADBLSpecmanOpButton} for the non-button op hierarchy.
 */
abstract class AbstractADBLSpecmanOp extends AbstractSpecmanOp {

  AbstractADBLSpecmanOp(Specman specman) {
    super(specman);
  }

  void run() {
    try (UndoRecording ur = specman.composeUndo()) {
      execute();
    }
    catch (EditException ex) {
      specman.showError(ex);
    }
  }

  abstract void execute() throws EditException;

}
