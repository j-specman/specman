package specman.undo.manager;

/** Implemented by undoable edits that show visual feedback while they are undone or redone (e.g. a
 * tooltip next to the changed element) and that have to hide it again when the user triggers the
 * <i>next</i> undo or redo.
 * <p>
 * {@link SpecmanUndoManager} takes care of the timing: when edit A has just been undone/redone and
 * the user then undoes/redoes edit B, the manager first calls {@link #hideFeedback()} on A and
 * only then processes B. */
public interface UndoRedoFeedbackHider {
  void hideFeedback();
}
