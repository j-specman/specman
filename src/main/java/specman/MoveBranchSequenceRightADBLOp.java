package specman;

import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceRightOperation;
import specman.view.AbstractSchrittView;

class MoveBranchSequenceRightADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  MoveBranchSequenceRightADBLOp(Specman specman, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(specman);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceRightOperation(step, initiatingFragment).execute();
  }

}
