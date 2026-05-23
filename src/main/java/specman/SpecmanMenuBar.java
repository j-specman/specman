package specman;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.graphics.ChangeColorSet;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import static specman.graphics.Styles.AENDERUNGSFARBE;
import static specman.graphics.Styles.CHANGESETS;

class SpecmanMenuBar extends JMenuBar {

  private final RecentFiles recentFiles;

  SpecmanMenuBar(Specman specman, HTMLEditorPane shefEditorPane) {
    recentFiles = new RecentFiles(specman);

    JMenuItem laden = new JMenuItem("Laden...");
    laden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    laden.addActionListener(e -> specman.diagrammLaden());

    JMenuItem speichern = new JMenuItem("Speichern");
    speichern.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
    speichern.addActionListener(e -> specman.diagrammSpeichern(false));

    JMenuItem speichernUnter = new JMenuItem("Speichern unter...");
    speichernUnter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    speichernUnter.addActionListener(e -> specman.diagrammSpeichern(true));

    JMenuItem exportAsPDF = new JMenuItem("Als PDF exportieren...");
    exportAsPDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
    exportAsPDF.addActionListener(e -> specman.exportAsPDF());

    JMenuItem exportAsGraphviz = new JMenuItem("Als Graphviz exportieren");
    exportAsGraphviz.addActionListener(e -> specman.exportAsGraphviz());

    JMenuItem exit = new JMenuItem("Beenden");
    exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    exit.addActionListener(e -> specman.dispatchEvent(new WindowEvent(specman, WindowEvent.WINDOW_CLOSING)));

    JMenu dateiMenu = new JMenu("Datei");
    dateiMenu.add(laden);
    dateiMenu.add(recentFiles.menu());
    dateiMenu.add(speichern);
    dateiMenu.add(speichernUnter);
    dateiMenu.add(exportAsPDF);
    dateiMenu.add(exportAsGraphviz);
    dateiMenu.add(exit);

    JMenu aenderungsfarbenMenu = new JMenu("Änderungsfarbe");
    aenderungsfarbenMenu.setIcon(new ColorDotIcon(AENDERUNGSFARBE.text.color));
    for (ChangeColorSet cs : CHANGESETS) {
      JMenuItem item = new JMenuItem(new ColorDotIcon(cs.text.color));
      item.addActionListener(e -> specman.fehler("Work in progress"));
      aenderungsfarbenMenu.add(item);
    }

    add(dateiMenu);
    add(shefEditorPane.getEditMenu());
    add(shefEditorPane.getFormatMenu());
    add(shefEditorPane.getInsertMenu());
    add(aenderungsfarbenMenu);
  }

  void addRecentFile(File file) {
    recentFiles.add(file);
  }

}
