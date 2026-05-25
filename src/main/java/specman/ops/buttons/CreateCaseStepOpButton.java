package specman.ops.buttons;

import specman.*;

import specman.ops.SpecmanOpContext;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateCaseStepOpButton extends AbstractCreateStepOpButton {

  public CreateCaseStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.caseSchrittZwischenschieben(After, ref, context);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.caseSchrittAnhaengen(context);
  }

}
