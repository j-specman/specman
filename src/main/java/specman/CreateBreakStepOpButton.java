package specman;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

class CreateBreakStepOpButton extends AbstractCreateStepOpButton {

  CreateBreakStepOpButton(Specman specman) { super(specman); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.breakSchrittZwischenschieben(After, ref, specman);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.breakSchrittAnhaengen(specman);
  }

}
