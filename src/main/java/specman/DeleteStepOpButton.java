package specman;

import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;

class DeleteStepOpButton extends AbstractSpecmanOpButton {

  DeleteStepOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (specman.lastFocusedTextArea == null) {
      return;
    }
    AbstractSchrittView step = findStep(specman.lastFocusedTextArea);
    specman.deleteStepADBL(step, specman.lastFocusedTextArea);
  }

  private AbstractSchrittView findStep(TextEditArea initiatingTextArea) throws EditException {
    AbstractSchrittView schritt = specman.findeSchritt(initiatingTextArea);
    if (schritt == null) {
      throw new EditException("Ups - niemandem scheint das Feld zu gehören, in dem steht: " + initiatingTextArea.getText());
    }
    return schritt;
  }

}
