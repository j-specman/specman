package classman.editor;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/** A small filled rectangle in a color, with a thin border, for buttons which show a color. */
class ColorSwatchIcon implements Icon {
  private static final int WIDTH = 32;
  private static final int HEIGHT = 14;

  private final Color color;

  /** @param color a color like #f2e9c4 */
  ColorSwatchIcon(String color) {
    this.color = Color.decode(color);
  }

  @Override
  public void paintIcon(Component component, Graphics g, int x, int y) {
    g.setColor(color);
    g.fillRect(x, y, WIDTH, HEIGHT);
    g.setColor(Color.GRAY);
    g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
  }

  @Override
  public int getIconWidth() {
    return WIDTH;
  }

  @Override
  public int getIconHeight() {
    return HEIGHT;
  }
}
