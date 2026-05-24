package specman.opbuttons;

import specman.*;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateWhileWhileStepOpButton extends AbstractCreateStepOpButton {

  public CreateWhileWhileStepOpButton(Specman context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.whileWhileSchrittZwischenschieben(After, ref, context);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.whileWhileSchrittAnhaengen(context);
  }

}
