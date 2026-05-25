package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.InteractiveStepFragment;
import specman.view.AbstractSchrittView;

public class UndoableBranchSequenceMovedLeft extends AbstractUndoableInteraction {
  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public UndoableBranchSequenceMovedLeft(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  protected void undoEdit() throws EditException {
    Specman.instance().moveBranchSequenceRightADBL(step, initiatingFragment);
  }

  @Override
  protected void redoEdit() throws EditException {
    Specman.instance().moveBranchSequenceLeftADBL(step, initiatingFragment);
  }
}
