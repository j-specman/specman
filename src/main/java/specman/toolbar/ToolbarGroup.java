package specman.toolbar;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.util.List;

import static specman.i18n.Messages.i18n;

/**
 * One titled function group of the {@link RibbonToolBar}: the controls on top, the group title
 * centered below, and a vertical separator line on the right edge. The line has the color of the other borders and
 * keeps a distance to the top and bottom.
 */
public class ToolbarGroup extends JPanel {

  private static final Color TITLE_COLOR = new Color(0x70, 0x70, 0x70);

  private final JPanel content = new JPanel(new GridLayout(0, 1));
  private JPanel currentRow;

  /** @param titleKey i18n key of the group title, e.g. {@code toolbar.group.file} */
  public ToolbarGroup(String titleKey) {
    super(new BorderLayout());
    setOpaque(false);
    content.setOpaque(false);
    nextRow();

    JLabel title = new JLabel(i18n(titleKey), SwingConstants.CENTER);
    title.setFont(UIManager.getFont("MenuBar.font"));
    title.setForeground(TITLE_COLOR);

    add(content, BorderLayout.CENTER);
    add(title, BorderLayout.SOUTH);
    setBorder(new SeparatorBorder());
    setAlignmentY(TOP_ALIGNMENT);
  }

  /** Never wider than needed, so that a wide toolbar leaves the free space at its right end. Height may grow. */
  @Override
  public Dimension getMaximumSize() {
    return new Dimension(getPreferredSize().width, Short.MAX_VALUE);
  }

  /** Adds a control to the content area; {@code add(Component)} on the group itself would
   * replace the layout structure, so this is the only way to fill it. */
  public Component addControl(Component component) {
    if (component instanceof AbstractButton abstractButton) {
      // flat look without button border, as in the former JToolBar (FlatLaf only)
      abstractButton.putClientProperty("JButton.buttonType", "toolBarButton");
    }
    return currentRow.add(component);
  }

  /** Starts a new row below the current one; the following controls are added there. */
  public void nextRow() {
    currentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
    currentRow.setOpaque(false);
    content.add(currentRow);
  }

  /** Adds the controls, the first {@code firstRowSize} of them in the current row and the rest in a new one. */
  public void addControlsInTwoRows(List<? extends Component> controls, int firstRowSize) {
    controls.subList(0, firstRowSize).forEach(this::addControl);
    nextRow();
    controls.subList(firstRowSize, controls.size()).forEach(this::addControl);
  }

}
