package specman;

import specman.view.AbstractSchrittView;
import specman.view.CatchUeberschrift;

class CreateCatchStepOpButton extends AbstractUDBLSpecmanOpButton {

  CreateCatchStepOpButton(Specman specman) { super(specman); }

  @Override
  void execute() throws EditException {
    AbstractSchrittView referenceStep = specman.hauptSequenz.findeSchritt(specman.lastFocusedTextArea);
    CatchUeberschrift referenceCatchHeading = specman.lastFocusedTextArea.containingCatchHeading();
    if (referenceStep != null) {
      specman.dropWelcomeMessage();
      new CatchLinkDialog(null, referenceStep.getParent(), referenceCatchHeading);
    }
  }

}
