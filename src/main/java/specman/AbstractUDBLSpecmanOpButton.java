package specman;

import specman.undo.manager.UndoRecording;

/**
 * Base class for toolbar buttons whose operation must be recorded as a single
 * atomic undo step. Wraps {@link #execute()} in {@link Specman#composeUndo()}.
 */
abstract class AbstractUDBLSpecmanOpButton extends AbstractSpecmanOpButton {

  AbstractUDBLSpecmanOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void registerActionListener() {
    addActionListener(e -> {
      try (UndoRecording ur = specman.composeUndo()) {
        execute();
      }
      catch (EditException ex) {
        specman.showError(ex);
      }
    });
  }

}
