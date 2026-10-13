package specman.graphics;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Square {@link Icon} rendering a vector graphic from {@code images/<name>.svg} via FlatLaf
 * Extras, instead of a bitmap - stays crisp at any zoom level rather than pixelating like
 * {@link IconReader}'s PNG icons do when scaled up.
 * <p>
 * Size is mutable (not fixed at construction) so a single instance can be rescaled in place as
 * the editor's zoom factor changes, the same way HangingTab widgets rescale their own font/border
 * rather than being rebuilt from scratch.
 */
public class SvgIcon implements Icon, FlatLaf.DisabledIconProvider {

  private FlatSVGIcon icon;
  private int size;

  public SvgIcon(String iconBasename, int size) {
    this(loadResource(iconBasename), size);
  }

  /** Icon from SVG markup held in memory, e.g. read from a user-chosen file or a model file. */
  public SvgIcon(int size, String svgContent) {
    this(loadContent(svgContent), size);
  }

  private SvgIcon(FlatSVGIcon icon, int size) {
    this.icon = icon.derive(size, size);
    this.size = size;
  }

  private static FlatSVGIcon loadResource(String iconBasename) {
    String resource = "images/" + iconBasename + ".svg";
    URL url = SvgIcon.class.getClassLoader().getResource(resource);
    if (url == null) {
      throw new IllegalArgumentException("Can't load SVG icon " + resource);
    }
    return new FlatSVGIcon(url);
  }

  private static FlatSVGIcon loadContent(String svgContent) {
    try {
      FlatSVGIcon icon = new FlatSVGIcon(new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8)));
      if (!icon.hasFound()) {
        throw new IllegalArgumentException("Not a valid SVG graphic");
      }
      return icon;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void setSize(int size) {
    this.size = size;
    icon = icon.derive(size, size);
  }

  /** Swing creates disabled icons on its own only for ImageIcons - FlatLaf asks this interface
   * instead, so e.g. a disabled menu item gets a grayed-out icon rather than none at all. */
  @Override
  public Icon getDisabledIcon() {
    return icon.getDisabledIcon();
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
