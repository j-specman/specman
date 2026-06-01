package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.ops.SpecmanOpContext;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.SchrittSequenzView;

import static specman.view.RelativeStepPosition.After;

public class CreateSimpleStepOpButton extends AbstractCreateStepOpButton {

  public CreateSimpleStepOpButton(SpecmanOpContext context) { super(context); }

  @Override AbstractSchrittView insertAfter(SchrittSequenzView seq, AbstractSchrittView ref) throws EditException {
    return seq.einfachenSchrittZwischenschieben(After, ref);
  }

  @Override AbstractSchrittView append(SchrittSequenzView seq) throws EditException {
    return seq.einfachenSchrittAnhaengen();
  }

  @Override public DragSource dragSource() { return new DragSource.NewStep(EinfacherSchrittView.class); }

}
