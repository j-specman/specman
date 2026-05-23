package specman;

import specman.undo.manager.UndoRecording;

/**
 * Base class for Specman operations that must be recorded as a single atomic undo step.
 * Wraps {@link #execute()} in {@link Specman#composeUndo()} and handles {@link EditException}.
 * Parallel to {@link AbstractUDBLSpecmanOpButton} for the non-button op hierarchy.
 */
abstract class AbstractUDBLSpecmanOp extends AbstractSpecmanOp {

  AbstractUDBLSpecmanOp(Specman specman) {
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
