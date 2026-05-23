package specman;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

abstract class AbstractCreateStepOpButton extends AbstractADBLSpecmanOpButton {

  AbstractCreateStepOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    specman.dropWelcomeMessage();
    AbstractSchrittView referenceStep = specman.hauptSequenz.findeSchritt(specman.lastFocusedTextArea);
    AbstractSchrittView newStep = (referenceStep != null)
        ? insertAfter(referenceStep.getParent(), referenceStep)
        : append(specman.hauptSequenz);
    specman.newStepPostInit(newStep);
  }

  abstract AbstractSchrittView insertAfter(SchrittSequenzView sequence, AbstractSchrittView referenceStep) throws EditException;

  abstract AbstractSchrittView append(SchrittSequenzView sequence) throws EditException;

}
