package specman.menubar;

import specman.ChangeSet;

import javax.swing.*;
import java.awt.*;

class ChangeSetDotIcon implements Icon {
  private static final int SIZE = 10;
  private final Color color;

  ChangeSetDotIcon(ChangeSet changeSet) {
    this.color = changeSet.menuColor();
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(color);
    g2.fillOval(x, y, SIZE, SIZE);
    g2.setColor(color.darker());
    g2.drawOval(x, y, SIZE, SIZE);
    g2.dispose();
  }

  @Override
  public int getIconWidth() {
    return SIZE;
  }

  @Override
  public int getIconHeight() {
    return SIZE;
  }
}
