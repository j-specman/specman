package specman.menubar;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.jetbrains.annotations.NotNull;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.i18n.IMenu;
import specman.i18n.IMenuItem;
import specman.ops.SpecmanOpContext;

import javax.swing.*;
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
  public static final String OPEN_LINK_ITEM = "menu.file.open.link";
  public static final String ALIASES_ITEM = "menu.file.aliases";
  public static final String META_TAGS_ITEM = "menu.file.meta.tags";
  public static final String SETTINGS_ITEM = "menu.file.settings";
  public static final String EXIT_ITEM = "menu.file.exit";

  private final RecentFiles recentFiles;

  public SpecmanMenuBar(SpecmanOpContext context, HTMLEditorPane shefEditorPane) {
    recentFiles = new RecentFiles();
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));

    JMenu fileMenu = createFileMenu(context);
    add(fileMenu);

    add(shefEditorPane.getEditMenu());
    add(shefEditorPane.getFormatMenu());
    add(shefEditorPane.getInsertMenu());
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

    JMenuItem openLink = new IMenuItem(OPEN_LINK_ITEM);
    openLink.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
    openLink.addActionListener(e -> context.navigateToLinkInClipboard());

    JMenuItem exit = new IMenuItem(EXIT_ITEM, icon(SpecmanIcons.EXIT));
    exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
    exit.addActionListener(e -> context.exit());

    JMenuItem metaTags = new IMenuItem(META_TAGS_ITEM, icon(SpecmanIcons.META_TAGS));
    metaTags.addActionListener(e -> context.openMetaTagConfigs());

    JMenuItem aliases = new IMenuItem(ALIASES_ITEM);
    aliases.addActionListener(e -> context.openAliases());

    JMenuItem settings = new IMenuItem(SETTINGS_ITEM, icon(SpecmanIcons.SETTINGS));
    settings.addActionListener(e -> context.openSettings());

    JMenu dateiMenu = new IMenu(FILE_MENU);
    dateiMenu.add(neu);
    dateiMenu.add(laden);
    recentFiles.menu().setIcon(icon(SpecmanIcons.FILE_RECENT));
    dateiMenu.add(recentFiles.menu());
    dateiMenu.add(openLink);
    dateiMenu.add(speichern);
    dateiMenu.add(speichernUnter);
    dateiMenu.add(exportAsPDF);
    dateiMenu.add(exportAsGraphviz);
    dateiMenu.addSeparator();
    dateiMenu.add(metaTags);
    dateiMenu.add(aliases);
    dateiMenu.add(settings);
    dateiMenu.add(exit);
    return dateiMenu;
  }

  private static Icon icon(String svgPath) {
    return IconReader.readSvgIcon(svgPath, SpecmanIcons.SIZE_SMALL);
  }

  public void addRecentFile(File file) {
    recentFiles.add(file);
  }

}
