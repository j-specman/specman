package specman;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;

class SpecmanMenuBar extends JMenuBar {

  private final RecentFiles recentFiles;
  private JMenu changesetMenu;

  SpecmanMenuBar(Specman specman, HTMLEditorPane shefEditorPane) {
    recentFiles = new RecentFiles();

    JMenu fileMenu = createFileMenu(specman);
    add(fileMenu);

    add(shefEditorPane.getEditMenu());
    add(shefEditorPane.getFormatMenu());
    add(shefEditorPane.getInsertMenu());

    createChangesetMenu(specman);
    add(changesetMenu);
  }

  private @NotNull JMenu createFileMenu(Specman specman) {
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

    JMenu dateiMenu = new JMenu("File");
    dateiMenu.add(laden);
    dateiMenu.add(recentFiles.menu());
    dateiMenu.add(speichern);
    dateiMenu.add(speichernUnter);
    dateiMenu.add(exportAsPDF);
    dateiMenu.add(exportAsGraphviz);
    dateiMenu.add(exit);
    return dateiMenu;
  }

  private void createChangesetMenu(Specman specman) {
    changesetMenu = new JMenu("Change Set");
    changesetMenu.setIcon(new ChangeSetDotIcon(specman.currentChangeSet));
    for (ChangeSet cs : ChangeSet.ALL.values()) {
      String label = "<html><table cellspacing='0' cellpadding='0'><tr><td bgcolor='"
          + cs.textHtmlColor() + "'>" + cs.name + "</td></tr></table></html>";
      JMenuItem item = new JMenuItem(label, new ChangeSetDotIcon(cs));
      item.setBackground(cs.panelColor());
      item.setForeground(Color.BLACK);
      item.setOpaque(true);
      item.addActionListener(e -> specman.updateChangeSet(cs));
      changesetMenu.add(item);
    }
  }

  void addRecentFile(File file) {
    recentFiles.add(file);
  }

  void updateChangeSet(ChangeSet changeSet) {
    changesetMenu.setIcon(new ChangeSetDotIcon(changeSet));
  }

}
