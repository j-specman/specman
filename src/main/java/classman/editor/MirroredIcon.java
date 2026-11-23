package classman.editor;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** An icon turned around the vertical axis, so that the diamond or arrowhead of a line which is at its left is at its right.
 * The look and feel makes the icon of a menu entry which can't be chosen from the icon itself, if it can: without that,
 * the entry would have no icon at all. */
class MirroredIcon implements Icon, FlatLaf.DisabledIconProvider {
  private final Icon icon;

  MirroredIcon(Icon icon) {
    this.icon = icon;
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.translate(x + icon.getIconWidth(), y);
      g.scale(-1, 1);
      icon.paintIcon(component, g, 0, 0);
    }
    finally {
      g.dispose();
    }
  }

  /** @return the same icon in the look of what can't be chosen, null if the icon can't make it */
  @Override
  public Icon getDisabledIcon() {
    return icon instanceof FlatLaf.DisabledIconProvider provider && provider.getDisabledIcon() != null
      ? new MirroredIcon(provider.getDisabledIcon())
      : null;
  }

  @Override
  public int getIconWidth() { return icon.getIconWidth(); }

  @Override
  public int getIconHeight() { return icon.getIconHeight(); }
}
