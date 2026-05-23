package specman;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

class CreateSimpleStepOpButton extends AbstractCreateStepOpButton {

  CreateSimpleStepOpButton(Specman specman) { super(specman); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.einfachenSchrittZwischenschieben(After, ref, specman);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.einfachenSchrittAnhaengen(specman);
  }

}
