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
    Color aktuelleHintergrundfarbe = schritt.getBackground();
    int farbwert = aktuelleHintergrundfarbe.getRed() == 240 ? 255 : 240;
    Color neueHintergrundfarbe = new Color(farbwert, farbwert, farbwert);
    schritt.setBackgroundUDBL(neueHintergrundfarbe);
    addEdit(new UndoableSchrittEingefaerbt(schritt, aktuelleHintergrundfarbe, neueHintergrundfarbe));
  }

}
