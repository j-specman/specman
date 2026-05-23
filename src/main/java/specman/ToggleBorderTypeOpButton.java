package specman;

import specman.undo.UndoableToggleStepBorder;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

class ToggleBorderTypeOpButton extends AbstractUDBLSpecmanOpButton {

  ToggleBorderTypeOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    AbstractSchrittView schritt = specman.hauptSequenz.findeSchritt(specman.lastFocusedTextArea);
    if (schritt == null) {
      return;
    }
    SchrittSequenzView sequenz = schritt.getParent();
    sequenz.toggleBorderType(schritt);
    specman.addEdit(new UndoableToggleStepBorder(specman, schritt, sequenz));
    specman.diagrammAktualisieren(schritt.getFirstEditArea());
  }

}
