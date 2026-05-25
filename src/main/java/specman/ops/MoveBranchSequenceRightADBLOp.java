package specman.ops;

import specman.EditException;
import specman.SpecmanOpContext;
import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceRightOperation;
import specman.view.AbstractSchrittView;

public class MoveBranchSequenceRightADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public MoveBranchSequenceRightADBLOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceRightOperation(step, initiatingFragment).execute();
  }

}
