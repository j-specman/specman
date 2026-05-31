package specman.menubar;

import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.*;

class ChangeSetMenuItemUI extends BasicMenuItemUI {

  @Override
  protected void paintBackground(Graphics g, JMenuItem item, Color bgColor) {
    g.setColor(item.getBackground());
    g.fillRect(0, 0, item.getWidth(), item.getHeight());
    if (item.getModel().isArmed()) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(Color.GRAY);
      g2.setStroke(new BasicStroke(1.0f));
      g2.drawRect(1, 1, item.getWidth() - 3, item.getHeight() - 3);
    }
  }
}
