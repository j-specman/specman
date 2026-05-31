package specman.menubar;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.jetbrains.annotations.NotNull;
import specman.ChangeSet;
import specman.ops.SpecmanOpContext;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;

public class SpecmanMenuBar extends JMenuBar {

  private final RecentFiles recentFiles;
  private JMenu changesetMenu;

  public SpecmanMenuBar(SpecmanOpContext context, HTMLEditorPane shefEditorPane) {
    recentFiles = new RecentFiles();

    JMenu fileMenu = createFileMenu(context);
    add(fileMenu);

    add(shefEditorPane.getEditMenu());
    add(shefEditorPane.getFormatMenu());
    add(shefEditorPane.getInsertMenu());

    createChangesetMenu(context);
    add(changesetMenu);
  }

  private @NotNull JMenu createFileMenu(SpecmanOpContext context) {
    JMenuItem laden = new JMenuItem("Laden...");
    laden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    laden.addActionListener(e -> context.diagrammLaden());

    JMenuItem speichern = new JMenuItem("Speichern");
    speichern.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
    speichern.addActionListener(e -> context.diagrammSpeichern(false));

    JMenuItem speichernUnter = new JMenuItem("Speichern unter...");
    speichernUnter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    speichernUnter.addActionListener(e -> context.diagrammSpeichern(true));

    JMenuItem exportAsPDF = new JMenuItem("Als PDF exportieren...");
    exportAsPDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
    exportAsPDF.addActionListener(e -> context.exportAsPDF());

    JMenuItem exportAsGraphviz = new JMenuItem("Als Graphviz exportieren");
    exportAsGraphviz.addActionListener(e -> context.exportAsGraphviz());

    JMenuItem exit = new JMenuItem("Beenden");
    exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    exit.addActionListener(e -> context.exit());

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

  /**
   * Builds the Change Set selection menu. Each item is styled to give the user an immediate
   * preview of the change-tracking colors:
   * <ul>
   *   <li>The item background uses the change set's panel color (a light pastel).</li>
   *   <li>On hover, the item label switches to an HTML label that highlights the name text
   *       with the change set's text background color — the same color used to mark changed
   *       content in the diagram. A {@code ChangeListener} drives this switch so that the
   *       colored highlight only appears while the mouse is over the item, keeping the menu
   *       visually calm when closed.</li>
   *   <li>A custom {@link ChangeSetMenuItemUI} suppresses the L&F's default blue selection
   *       background and replaces it with a thin border, so the change-set colors remain
   *       dominant during navigation.</li>
   * </ul>
   */
  private void createChangesetMenu(SpecmanOpContext context) {
    changesetMenu = new JMenu("Change Set");
    changesetMenu.setIcon(new ChangeSetDotIcon(ChangeSet.changeset()));
    for (ChangeSet cs : ChangeSet.ALL.values()) {
      String hoveredLabel = "<html><table cellspacing='0' cellpadding='0'><tr><td bgcolor='"
          + cs.textHtmlColor() + "'>" + cs.name + "</td></tr></table></html>";
      JMenuItem item = new JMenuItem(cs.name, new ChangeSetDotIcon(cs));
      item.getModel().addChangeListener(e ->
          item.setText(item.getModel().isArmed() ? hoveredLabel : cs.name));
      item.setBackground(cs.panelColor());
      item.setForeground(Color.BLACK);
      item.setOpaque(true);
      item.setUI(new ChangeSetMenuItemUI());
      item.addActionListener(e -> context.updateChangeSet(cs));
      changesetMenu.add(item);
    }
  }

  public void addRecentFile(File file) {
    recentFiles.add(file);
  }

  public void updateChangeSet(ChangeSet changeSet) {
    changesetMenu.setIcon(new ChangeSetDotIcon(changeSet));
  }

}
