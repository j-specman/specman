package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.ops.SpecmanOpContext;
import specman.view.SubsequenzSchrittView;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateSubsequenceStepOpButton extends AbstractCreateStepOpButton {

  public CreateSubsequenceStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.subsequenzSchrittZwischenschieben(After, ref);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.subsequenzSchrittAnhaengen();
  }


  @Override public DragSource dragSource() { return new DragSource.StepCreation(SubsequenzSchrittView.class); }

}
