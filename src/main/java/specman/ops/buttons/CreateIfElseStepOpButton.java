package specman.ops.buttons;

import specman.*;

import specman.ops.SpecmanOpContext;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateIfElseStepOpButton extends AbstractCreateStepOpButton {

  public CreateIfElseStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.ifElseSchrittZwischenschieben(After, ref, context);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.ifElseSchrittAnhaengen(context);
  }

}
