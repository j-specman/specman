package specman.menubar;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.jetbrains.annotations.NotNull;
import specman.ChangeSet;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.i18n.IMenu;
import specman.i18n.IMenuItem;
import specman.ops.SpecmanOpContext;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;

public class SpecmanMenuBar extends JMenuBar {

  public static final String FILE_MENU = "menu.file";
  public static final String NEW_ITEM = "menu.file.new";
  public static final String LOAD_ITEM = "menu.file.load";
  public static final String SAVE_ITEM = "menu.file.save";
  public static final String SAVE_AS_ITEM = "menu.file.save.as";
  public static final String EXPORT_PDF_ITEM = "menu.file.export.pdf";
  public static final String EXPORT_GRAPHVIZ_ITEM = "menu.file.export.graphviz";
  public static final String META_TAGS_ITEM = "menu.file.meta.tags";
  public static final String SETTINGS_ITEM = "menu.file.settings";
  public static final String EXIT_ITEM = "menu.file.exit";
  public static final String CHANGESET_MENU = "menu.changeset";

  private final RecentFiles recentFiles;
  private JMenu changesetMenu;

  public SpecmanMenuBar(SpecmanOpContext context, HTMLEditorPane shefEditorPane) {
    recentFiles = new RecentFiles();
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));

    JMenu fileMenu = createFileMenu(context);
    add(fileMenu);

    add(shefEditorPane.getEditMenu());
    add(shefEditorPane.getFormatMenu());
    add(shefEditorPane.getInsertMenu());
    add(createViewMenu(context));

    createChangesetMenu(context);
    add(changesetMenu);
  }

  private @NotNull JMenu createFileMenu(SpecmanOpContext context) {
    JMenuItem neu = new IMenuItem(NEW_ITEM, icon(SpecmanIcons.FILE_NEW));
    neu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
    neu.addActionListener(e -> context.diagrammNeu());

    JMenuItem laden = new IMenuItem(LOAD_ITEM, icon(SpecmanIcons.FILE_OPEN));
    laden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    laden.addActionListener(e -> context.diagrammLaden());

    JMenuItem speichern = new IMenuItem(SAVE_ITEM, icon(SpecmanIcons.FILE_SAVE));
    speichern.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
    speichern.addActionListener(e -> context.diagrammSpeichern(false));

    JMenuItem speichernUnter = new IMenuItem(SAVE_AS_ITEM, icon(SpecmanIcons.FILE_SAVE_AS));
    speichernUnter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    speichernUnter.addActionListener(e -> context.diagrammSpeichern(true));

    JMenuItem exportAsPDF = new IMenuItem(EXPORT_PDF_ITEM, icon(SpecmanIcons.PDF));
    exportAsPDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
    exportAsPDF.addActionListener(e -> context.exportAsPDF());

    JMenuItem exportAsGraphviz = new IMenuItem(EXPORT_GRAPHVIZ_ITEM, icon(SpecmanIcons.EXPORT_GRAPHVIZ));
    exportAsGraphviz.addActionListener(e -> context.exportAsGraphviz());

    JMenuItem exit = new IMenuItem(EXIT_ITEM, icon(SpecmanIcons.EXIT));
    exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    exit.addActionListener(e -> context.exit());

    JMenuItem metaTags = new IMenuItem(META_TAGS_ITEM, icon(SpecmanIcons.META_TAGS));
    metaTags.addActionListener(e -> context.openMetaTagConfigs());

    JMenuItem settings = new IMenuItem(SETTINGS_ITEM, icon(SpecmanIcons.SETTINGS));
    settings.addActionListener(e -> context.openSettings());

    JMenu dateiMenu = new IMenu(FILE_MENU);
    dateiMenu.add(neu);
    dateiMenu.add(laden);
    recentFiles.menu().setIcon(icon(SpecmanIcons.FILE_RECENT));
    dateiMenu.add(recentFiles.menu());
    dateiMenu.add(speichern);
    dateiMenu.add(speichernUnter);
    dateiMenu.add(exportAsPDF);
    dateiMenu.add(exportAsGraphviz);
    dateiMenu.addSeparator();
    dateiMenu.add(metaTags);
    dateiMenu.add(settings);
    dateiMenu.add(exit);
    return dateiMenu;
  }

  private static JMenu createViewMenu(SpecmanOpContext context) {
    JMenuItem collapseAll = new IMenuItem("menu.view.collapse.all", icon(SpecmanIcons.COLLAPSE_ALL));
    collapseAll.addActionListener(e -> context.alleSchritteKlappen(true));

    JMenuItem expandAll = new IMenuItem("menu.view.expand.all", icon(SpecmanIcons.EXPAND_ALL));
    expandAll.addActionListener(e -> context.alleSchritteKlappen(false));

    JMenu viewMenu = new IMenu("menu.view");
    viewMenu.add(collapseAll);
    viewMenu.add(expandAll);
    return viewMenu;
  }

  private static Icon icon(String svgPath) {
    return IconReader.readSvgIcon(svgPath, SpecmanIcons.SIZE_SMALL);
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
    changesetMenu = new IMenu(CHANGESET_MENU);
    changesetMenu.setIcon(new ChangeSetDotIcon(ChangeSet.changeset()));
    for (ChangeSet cs : ChangeSet.ALL.values()) {
      String hoveredLabel = "<html><table cellspacing='0' cellpadding='0'><tr><td bgcolor='"
          + cs.textHtmlColor() + "'>" + cs.name + "</td></tr></table></html>";
      JMenuItem item = new JMenuItem(cs.name, new ChangeSetDotIcon(cs));
      item.getModel().addChangeListener(e ->
          item.setText(item.getModel().isArmed() ? hoveredLabel : cs.name));
      item.setName(changeSetItemName(cs));
      item.setBackground(cs.panelColor());
      item.setForeground(Color.BLACK);
      item.setOpaque(true);
      item.setUI(new ChangeSetMenuItemUI());
      item.addActionListener(e -> context.updateChangeSet(cs));
      changesetMenu.add(item);
    }
  }

  /** The name of the menu item for choosing the given change set. Unlike the other menu items, there is no constant for it. */
  public static String changeSetItemName(ChangeSet changeSet) {
    return CHANGESET_MENU + "." + changeSet.name;
  }

  public void addRecentFile(File file) {
    recentFiles.add(file);
  }

  public void updateChangeSet(ChangeSet changeSet) {
    changesetMenu.setIcon(new ChangeSetDotIcon(changeSet));
  }

}
