package specman;

import specman.undo.UndoableSchrittEingefaerbt;
import specman.view.AbstractSchrittView;

import java.awt.*;

class ToneOpButton extends AbstractADBLSpecmanOpButton {

  ToneOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (specman.lastFocusedTextArea == null) {
      return;
    }
    AbstractSchrittView schritt = specman.getHauptSequenz().findeSchritt(specman.lastFocusedTextArea);
    Color aktuelleHintergrundfarbe = schritt.getBackground();
    int farbwert = aktuelleHintergrundfarbe.getRed() == 240 ? 255 : 240;
    Color neueHintergrundfarbe = new Color(farbwert, farbwert, farbwert);
    schritt.setBackgroundUDBL(neueHintergrundfarbe);
    specman.addEdit(new UndoableSchrittEingefaerbt(schritt, aktuelleHintergrundfarbe, neueHintergrundfarbe));
  }

}
