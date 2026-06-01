package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.ops.SpecmanOpContext;
import specman.view.CaseSchrittView;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateCaseStepOpButton extends AbstractCreateStepOpButton {

  public CreateCaseStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.caseSchrittZwischenschieben(After, ref);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.caseSchrittAnhaengen();
  }


  @Override public DragSource dragSource() { return new DragSource.NewStep(CaseSchrittView.class); }

}
