package specman;

import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceLeftOperation;
import specman.view.AbstractSchrittView;

class MoveBranchSequenceLeftADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  MoveBranchSequenceLeftADBLOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceLeftOperation(step, initiatingFragment).execute();
  }

}
