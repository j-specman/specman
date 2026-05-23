package specman;

import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceRightOperation;
import specman.view.AbstractSchrittView;

class MoveBranchSequenceRightSpecmanOp extends AbstractUDBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  MoveBranchSequenceRightSpecmanOp(Specman specman, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(specman);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceRightOperation(step, initiatingFragment).execute();
  }

}
