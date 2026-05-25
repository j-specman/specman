package specman.ops.buttons;

import specman.*;

import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;

public class DeleteStepOpButton extends AbstractSpecmanOpButton {

  public DeleteStepOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (getLastFocusedTextArea() == null) {
      return;
    }
    AbstractSchrittView step = findStep(getLastFocusedTextArea());
    deleteStepADBL(step, getLastFocusedTextArea());
  }

  private AbstractSchrittView findStep(TextEditArea initiatingTextArea) throws EditException {
    AbstractSchrittView schritt = findeSchritt(initiatingTextArea);
    if (schritt == null) {
      throw new EditException("Ups - niemandem scheint das Feld zu gehören, in dem steht: " + initiatingTextArea.getText());
    }
    return schritt;
  }

}
