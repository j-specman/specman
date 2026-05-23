package specman;

import specman.editarea.InteractiveStepFragment;
import specman.view.AbstractSchrittView;

class DeleteStepSpecmanOp extends AbstractUDBLSpecmanOp {

  private final AbstractSchrittView step;
  private final InteractiveStepFragment initiatingFragment;

  DeleteStepSpecmanOp(Specman specman, AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    super(specman);
    this.step = step;
    this.initiatingFragment = initiatingFragment;
  }

  @Override
  void execute() throws EditException {
    specman.loeschen.deleteStep(step, initiatingFragment);
  }

}
