package specman.ops.buttons;

import specman.*;

import specman.undo.UndoableSchrittEingefaerbt;
import specman.view.AbstractSchrittView;

import java.awt.*;

public class ToneOpButton extends AbstractADBLSpecmanOpButton {

  public ToneOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (getLastFocusedTextArea() == null) {
      return;
    }
    AbstractSchrittView schritt = getHauptSequenz().findeSchritt(getLastFocusedTextArea());
    // A keyboard shortcut can trigger this without the button being enabled, e.g. in intro or outro.
    if (schritt == null) {
      return;
    }
    Color alteShadeColor = schritt.getShadeColor();
    Color neueShadeColor = alteShadeColor != null ? null : new Color(220, 220, 220);
    schritt.setShadeColorUDBL(neueShadeColor);
    addEdit(new UndoableSchrittEingefaerbt(schritt, alteShadeColor, neueShadeColor));
  }

}
