package specman;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

class CreateIfElseStepOpButton extends AbstractCreateStepOpButton {

  CreateIfElseStepOpButton(Specman specman) { super(specman); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.ifElseSchrittZwischenschieben(After, ref, specman);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.ifElseSchrittAnhaengen(specman);
  }

}
