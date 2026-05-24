package specman.opbuttons;

import specman.*;

import specman.undo.UndoableToggleStepBorder;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

public class ToggleBorderTypeOpButton extends AbstractADBLSpecmanOpButton {

  public ToggleBorderTypeOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    AbstractSchrittView schritt = getHauptSequenz().findeSchritt(getLastFocusedTextArea());
    if (schritt == null) {
      return;
    }
    SchrittSequenzView sequenz = schritt.getParent();
    sequenz.toggleBorderType(schritt);
    addEdit(new UndoableToggleStepBorder(context, schritt, sequenz));
    diagrammAktualisieren(schritt.getFirstEditArea());
  }

}
