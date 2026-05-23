package specman;

import specman.undo.UndoableDiagrammSkaliert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

class ZoomComboBox extends JComboBox<ZoomFaktor> {

  ZoomComboBox(Specman specman) {
    for (ZoomFaktor faktor : ZoomFaktor.values()) {
      addItem(faktor);
    }
    setSelectedItem(ZoomFaktor.Faktor_100);
    setMaximumSize(new Dimension(65, 20));
    addActionListener(e -> {
      int prozentNeu = ((ZoomFaktor) getSelectedItem()).getProzent();
      int prozentAlt = specman.skalieren(prozentNeu);
      specman.undoManager.addEdit(new UndoableDiagrammSkaliert(specman, prozentAlt));
    });
  }

  // Updates the displayed zoom factor without triggering the action listener,
  // which would create an unwanted undo entry during programmatic updates (undo/redo, file load).
  void updateDisplay(int prozent) {
    ZoomFaktor faktor = ZoomFaktor.valueOf("Faktor_" + prozent);
    List<ActionListener> listeners = Arrays.asList(getActionListeners());
    listeners.forEach(this::removeActionListener);
    setSelectedItem(faktor);
    listeners.forEach(this::addActionListener);
  }

}
