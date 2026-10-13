package specman.metatag.generic.config;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/** A button showing a swatch of its current color, which opens a color chooser on click. A button
 * can also show "no color", as a crossed-out empty swatch. */
class ColorChooseButton extends JButton {
  private final String chooserTitle;
  private final Runnable onColorChanged;
  private Color color;

  /** @param initialColor the color to start with, null for no color
   * @param onColorChanged called after the user picked a different color */
  ColorChooseButton(String chooserTitle, Color initialColor, Runnable onColorChanged) {
    super("Choose...");
    this.chooserTitle = chooserTitle;
    this.onColorChanged = onColorChanged;
    setColor(initialColor);
    addActionListener(e -> chooseColor());
  }

  /** @return the chosen color, null for no color */
  Color getColor() {
    return color;
  }

  void setColor(Color color) {
    this.color = color;
    setIcon(new ColorSwatchIcon(color));
  }

  private void chooseColor() {
    Color chosen = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(this), chooserTitle, color);
    if (chosen != null) {
      setColor(chosen);
      onColorChanged.run();
    }
  }

  private static class ColorSwatchIcon implements Icon {
    private static final int SIZE = 14;
    private final Color color;

    ColorSwatchIcon(Color color) {
      this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      if (color != null) {
        g.setColor(color);
        g.fillRect(x, y, SIZE, SIZE);
      }
      g.setColor(Color.GRAY);
      g.drawRect(x, y, SIZE - 1, SIZE - 1);
      if (color == null) {
        g.drawLine(x, y + SIZE - 1, x + SIZE - 1, y);
      }
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
}
