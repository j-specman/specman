package specman.ops.buttons;

import specman.*;

import specman.ops.SpecmanOpContext;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateBreakStepOpButton extends AbstractCreateStepOpButton {

  public CreateBreakStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.breakSchrittZwischenschieben(After, ref, context);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.breakSchrittAnhaengen(context);
  }

}
