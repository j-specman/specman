package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.ops.SpecmanOpContext;
import specman.view.IfElseSchrittView;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateIfElseStepOpButton extends AbstractCreateStepOpButton {

  public CreateIfElseStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.ifElseSchrittZwischenschieben(After, ref);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.ifElseSchrittAnhaengen();
  }


  @Override public DragSource dragSource() { return new DragSource.StepCreation(IfElseSchrittView.class); }

}
