package specman.graphics;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.net.URL;

/**
 * Square {@link Icon} rendering a vector graphic from {@code images/<name>.svg} via FlatLaf
 * Extras, instead of a bitmap - stays crisp at any zoom level rather than pixelating like
 * {@link IconReader}'s PNG icons do when scaled up.
 * <p>
 * Size is mutable (not fixed at construction) so a single instance can be rescaled in place as
 * the editor's zoom factor changes, the same way HangingTab widgets rescale their own font/border
 * rather than being rebuilt from scratch.
 */
public class SvgIcon implements Icon {

  private FlatSVGIcon icon;
  private int size;

  public SvgIcon(String iconBasename, int size) {
    String resource = "images/" + iconBasename + ".svg";
    URL url = SvgIcon.class.getClassLoader().getResource(resource);
    if (url == null) {
      throw new IllegalArgumentException("Can't load SVG icon " + resource);
    }
    this.icon = new FlatSVGIcon(url).derive(size, size);
    this.size = size;
  }

  public void setSize(int size) {
    this.size = size;
    icon = icon.derive(size, size);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    icon.paintIcon(c, g, x, y);
  }

  @Override
  public int getIconWidth() {
    return size;
  }

  @Override
  public int getIconHeight() {
    return size;
  }
}
