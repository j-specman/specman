package specman;

import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceLeftOperation;
import specman.view.AbstractSchrittView;

class MoveBranchSequenceLeftSpecmanOp extends AbstractUDBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  MoveBranchSequenceLeftSpecmanOp(Specman specman, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(specman);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceLeftOperation(step, initiatingFragment).execute();
  }

}
