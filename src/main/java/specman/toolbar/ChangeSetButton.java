package specman.toolbar;

import specman.ChangeSet;
import specman.menubar.ChangeSetDotIcon;
import specman.menubar.ChangeSetMenuItemUI;
import specman.ops.SpecmanOpContext;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import static specman.i18n.Messages.i18n;

/**
 * Dropdown button for choosing the current change set: a dot in the color of the chosen change set, the label
 * "Change Set" and a dropdown arrow, on the change set's panel color. A click opens a popup with one colored item per change set.
 * Each item is styled to give the user an immediate preview of the change-tracking colors:
 * <ul>
 *   <li>The item background uses the change set's panel color (a light pastel).</li>
 *   <li>On hover, the item label switches to an HTML label that highlights the name text
 *       with the change set's text background color — the same color used to mark changed
 *       content in the diagram.</li>
 *   <li>A custom {@link ChangeSetMenuItemUI} suppresses the L&F's default blue selection
 *       background and replaces it with a thin border, so the change-set colors remain
 *       dominant during navigation.</li>
 * </ul>
 */
public class ChangeSetButton extends JButton {

  public static final String NAME = "toolbar.changeset";
  private static final String ITEM_NAME_PREFIX = "menu.changeset.";

  private final JPopupMenu popupMenu = new JPopupMenu();
  /** The button stands next to combo boxes, so it is as high as they are. */
  private final int comboBoxHeight = new JComboBox<>().getPreferredSize().height;

  public ChangeSetButton(SpecmanOpContext context) {
    setName(NAME);
    setText(i18n("menu.changeset") + " ▾");
    setToolTipText(i18n("menu.changeset"));
    setMargin(new Insets(0, 4, 0, 4));
    for (ChangeSet cs : ChangeSet.ALL.values()) {
      popupMenu.add(createItem(context, cs));
    }
    addActionListener(e -> popupMenu.show(this, 0, getHeight()));
    updateChangeSet(ChangeSet.changeset());
  }

  private static JMenuItem createItem(SpecmanOpContext context, ChangeSet cs) {
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
    return item;
  }

  /** The name of the popup item for choosing the given change set. */
  public static String changeSetItemName(ChangeSet changeSet) {
    return ITEM_NAME_PREFIX + changeSet.name;
  }

  public void updateChangeSet(ChangeSet changeSet) {
    setIcon(new ChangeSetDotIcon(changeSet));
    setBackground(changeSet.panelColor());
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    return new Dimension(size.width, Math.max(size.height, comboBoxHeight));
  }

  /** The popup with the change set items; the items are not children of the button. */
  public JPopupMenu popupMenu() {
    return popupMenu;
  }

}
