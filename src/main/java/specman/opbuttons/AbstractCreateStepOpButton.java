package specman.opbuttons;

import specman.*;

import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

public abstract class AbstractCreateStepOpButton extends AbstractADBLSpecmanOpButton {

  public AbstractCreateStepOpButton(SpecmanOpContext context) {
    super(context);
  }

  @Override
  void execute() throws EditException {
    dropWelcomeMessage();
    AbstractSchrittView referenceStep = getHauptSequenz().findeSchritt(getLastFocusedTextArea());
    AbstractSchrittView newStep = (referenceStep != null)
        ? insertAfter(referenceStep.getParent(), referenceStep)
        : append(getHauptSequenz());
    newStepPostInit(newStep);
  }

  abstract AbstractSchrittView insertAfter(SchrittSequenzView sequence, AbstractSchrittView referenceStep) throws EditException;

  abstract AbstractSchrittView append(SchrittSequenzView sequence) throws EditException;

}
