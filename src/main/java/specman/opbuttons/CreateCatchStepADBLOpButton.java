package specman.opbuttons;

import specman.*;

import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchUeberschrift;

public class CreateCatchStepADBLOpButton extends AbstractADBLSpecmanOpButton {

  public CreateCatchStepADBLOpButton(Specman specman) { super(specman); }

  @Override
  void execute() throws EditException {
    TextEditArea lastFocused = (TextEditArea) getLastFocusedTextArea();
    AbstractSchrittView referenceStep = getHauptSequenz().findeSchritt(lastFocused);
    CatchUeberschrift referenceCatchHeading = lastFocused.containingCatchHeading();
    if (referenceStep != null) {
      dropWelcomeMessage();
      new CatchLinkDialog(null, referenceStep.getParent(), referenceCatchHeading);
    }

  }

}
