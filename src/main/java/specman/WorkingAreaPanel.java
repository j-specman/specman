package specman;

import javax.swing.*;
import java.awt.*;

public class WorkingAreaPanel extends JPanel {
  private static final BasicStroke GESTRICHELTE_LINIE =
    new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1.0f, new float[] {10.0f, 10.0f }, 0f);

  private Integer dragX;

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (dragX != null) {
      Graphics2D g2 = (Graphics2D)g;
      g2.setStroke(GESTRICHELTE_LINIE);
      g.drawLine(dragX, 0, dragX, getHeight());
    }
  }

  public void showDragLine(Integer dragX) {
    this.dragX = dragX;
    repaint();
  }
}
