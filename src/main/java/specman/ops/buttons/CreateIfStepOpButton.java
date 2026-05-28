package specman.ops.buttons;

import specman.*;

import specman.ops.SpecmanOpContext;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateIfStepOpButton extends AbstractCreateStepOpButton {

  public CreateIfStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.ifSchrittZwischenschieben(After, ref);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.ifSchrittAnhaengen();
  }

}
