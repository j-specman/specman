package specman.ops;

import specman.EditException;
import specman.SpecmanOpContext;
import specman.editarea.InteractiveStepFragment;
import specman.modelops.MoveBranchSequenceLeftOperation;
import specman.view.AbstractSchrittView;

public class MoveBranchSequenceLeftADBLOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  public MoveBranchSequenceLeftADBLOp(SpecmanOpContext context, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(context);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    new MoveBranchSequenceLeftOperation(step, initiatingFragment).execute();
  }

}
