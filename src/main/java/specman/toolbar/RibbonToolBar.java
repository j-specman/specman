package specman.toolbar;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Container for {@link ToolbarGroup}s. Lays all groups out in a single row, all with the same height.
 * If the window is narrower than the groups together, put the bar into {@link #inScrollPane(Border)}:
 * it then scrolls horizontally instead of wrapping.
 */
public class RibbonToolBar extends JPanel {

  private static final int SCROLL_UNIT = 20;

  public RibbonToolBar() {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setOpaque(false);
  }

  public void addGroup(ToolbarGroup group) {
    add(group);
  }

  /**
   * Wraps this bar in a scroll pane with a horizontal scroll bar that only shows up when needed. The mouse wheel
   * scrolls horizontally. Scrolling replaced wrapping whole groups into a second row, which did not look good in the
   * real GUI (decided after a visual check). The pane's height includes the scroll bar when it is visible.
   */
  public JScrollPane inScrollPane(Border border) {
    JScrollPane scrollPane = createScrollPane();
    scrollPane.setBorder(border);
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
    scrollBar.setUnitIncrement(SCROLL_UNIT);
    scrollPane.addMouseWheelListener(e ->
      scrollBar.setValue(scrollBar.getValue() + e.getWheelRotation() * SCROLL_UNIT * 3));
    revalidateWhenScrollBarToggles(scrollPane);
    return scrollPane;
  }

  /**
   * Scroll pane around this bar whose preferred size is computed from the current width instead of the layout
   * state: the scroll bar's height counts exactly when the bar does not fit.
   */
  private JScrollPane createScrollPane() {
    RibbonToolBar bar = this;
    return new JScrollPane(bar,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
      @Override
      public Dimension getPreferredSize() {
        Dimension size = bar.getPreferredSize();
        Insets insets = getInsets();
        boolean scrollBarNeeded = size.width > getWidth() - insets.left - insets.right;
        int scrollBarHeight = scrollBarNeeded ? getHorizontalScrollBar().getPreferredSize().height : 0;
        return new Dimension(size.width + insets.left + insets.right,
          size.height + insets.top + insets.bottom + scrollBarHeight);
      }
    };
  }

  /**
   * The scroll bar appears and disappears depending on the width; a parent layout only learns
   * about the changed height if the preferred size is invalidated.
   */
  private static void revalidateWhenScrollBarToggles(JScrollPane scrollPane) {
    scrollPane.addComponentListener(new ComponentAdapter() {
      private int lastPreferredHeight = -1;

      @Override
      public void componentResized(ComponentEvent e) {
        int height = scrollPane.getPreferredSize().height;
        if (height != lastPreferredHeight) {
          lastPreferredHeight = height;
          scrollPane.revalidate();
        }
      }
    });
  }
}
