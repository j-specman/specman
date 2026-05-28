package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.InteractiveStepFragment;
import specman.view.AbstractSchrittView;
import static specman.Specman.editor;

public class UndoableBranchSequenceMovedRight extends AbstractUndoableInteraction {
  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public UndoableBranchSequenceMovedRight(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  protected void undoEdit() throws EditException {
    editor().moveBranchSequenceLeftADBL(step, initiatingFragment);
  }

  @Override
  protected void redoEdit() throws EditException {
    editor().moveBranchSequenceRightADBL(step, initiatingFragment);
  }
}
