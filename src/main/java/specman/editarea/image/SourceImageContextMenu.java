package specman.editarea.image;

import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.i18n.IMenuItem;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.MouseEvent;

/** The context menu of a diagram which is shown from its source. As for the step number, there is one shared instance
 * which is initialized for the diagram last right-clicked. */
public class SourceImageContextMenu {
  public static final String EDIT_ITEM = "menu.context.edit";
  public static final String IMPORT_ITEM = "menu.context.import";
  public static final String DELETE_ITEM = "menu.context.delete";

  private static final SourceImageContextMenu instance = new SourceImageContextMenu();

  private final JPopupMenu popup = new JPopupMenu();
  private AbstractSourceSvgImageEditArea currentDiagram;

  private SourceImageContextMenu() {
    JMenuItem edit = new IMenuItem(EDIT_ITEM);
    edit.addActionListener(e -> currentDiagram.editSource());
    JMenuItem importSource = new IMenuItem(IMPORT_ITEM);
    importSource.addActionListener(e -> currentDiagram.importSource());
    JMenuItem delete = new IMenuItem(DELETE_ITEM, IconReader.readSvgIcon(SpecmanIcons.DELETE, SpecmanIcons.SIZE_SMALL));
    delete.addActionListener(e -> currentDiagram.deleteUDBL());
    popup.add(edit);
    popup.add(importSource);
    popup.add(delete);
  }

  /** Opens the menu for the diagram at the position of the mouse event. */
  static void open(AbstractSourceSvgImageEditArea diagram, MouseEvent e) {
    instance.currentDiagram = diagram;
    // Not showing means headless in unit tests: The initialized menu stays operable via popupMenu()
    if (diagram.isShowing()) {
      instance.popup.show(diagram, e.getX(), e.getY());
    }
  }

  /** The menu as initialized for the diagram last right-clicked. Allows tests to operate the menu without showing it. */
  public static JPopupMenu popupMenu() {
    return instance.popup;
  }
}
