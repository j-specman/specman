package specman.graphics;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URL;

/**
 * Square {@link Icon} rendering a vector graphic from {@code images/<name>.svg} via JSVG, instead
 * of a bitmap - stays crisp at any zoom level rather than pixelating like {@link IconReader}'s
 * PNG icons do when scaled up.
 * <p>
 * Size is mutable (not fixed at construction) so a single instance can be rescaled in place as
 * the editor's zoom factor changes, the same way HangingTab widgets rescale their own font/border
 * rather than being rebuilt from scratch.
 */
public class SvgIcon implements Icon {

  private final SVGDocument document;
  private int size;

  public SvgIcon(String iconBasename, int size) {
    String resource = "images/" + iconBasename + ".svg";
    URL url = SvgIcon.class.getClassLoader().getResource(resource);
    if (url == null) {
      throw new IllegalArgumentException("Can't load SVG icon " + resource);
    }
    this.document = new SVGLoader().load(url);
    this.size = size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    document.render(c, g2, new ViewBox(x, y, size, size));
    g2.dispose();
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
