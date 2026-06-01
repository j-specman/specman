package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;
import specman.view.CatchUeberschrift;

public class CreateCatchStepADBLOpButton extends AbstractADBLSpecmanOpButton implements DragSourceProvider {

  public CreateCatchStepADBLOpButton(Specman specman) { super(specman); }

  public DragSource dragSource() { return new DragSource.StepCreation(CatchBereich.class); }

  @Override
  void execute() throws EditException {
    TextEditArea lastFocused = getLastFocusedTextArea();
    AbstractSchrittView referenceStep = getHauptSequenz().findeSchritt(lastFocused);
    CatchUeberschrift referenceCatchHeading = lastFocused.containingCatchHeading();
    if (referenceStep != null) {
      dropWelcomeMessage();
      new CatchLinkDialog(null, referenceStep.getParent(), referenceCatchHeading);
    }

  }

}
