package specman.ops;

import specman.EditException;
import specman.editarea.InteractiveStepFragment;
import specman.undo.UndoableBranchSequenceMovedLeft;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;

public class MoveBranchSequenceLeftADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public MoveBranchSequenceLeftADBLOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  /** Moving catch sequences right or left is not considered as a change being reflected in change recording. */
  @Override
  void execute() throws EditException {
    if (step instanceof CatchBereich) {
      CatchBereich catchBereich = (CatchBereich) step;
      catchBereich.moveCatchSequenceLeft(initiatingFragment);
    }
    addEdit(new UndoableBranchSequenceMovedLeft(step, initiatingFragment));
  }

}
