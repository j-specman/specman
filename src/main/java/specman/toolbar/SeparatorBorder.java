package specman.toolbar;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Vertical separator line on the right edge of a {@link ToolbarGroup}. The line has the color of the other
 * borders and keeps a distance to the top and bottom.
 */
final class SeparatorBorder extends AbstractBorder {

  private static final int SEPARATOR_MARGIN = 6;
  private static final int LINE_WIDTH = 1;
  private static final int PADDING = 3;

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Color color = UIManager.getColor("Component.borderColor");
    g.setColor(color != null ? color : Color.LIGHT_GRAY);
    g.fillRect(x + width - LINE_WIDTH, y + SEPARATOR_MARGIN, LINE_WIDTH, height - 2 * SEPARATOR_MARGIN);
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return new Insets(0, PADDING, 0, PADDING + LINE_WIDTH + PADDING);
  }
}
