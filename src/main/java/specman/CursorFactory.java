package specman;

import specman.graphics.IconReader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Einen eigenen Cursor bauen ist etwas komplizierter als man denkt, wenn man vermeiden möchte, dass Java das
 * vorgefertigte Icon wild skaliert. Man muss also vorher über {@link Toolkit#getBestCursorSize(int, int)}
 * feststellen, wie groß ein Cursorbild sein muss (meistens 32x32 oder 64x64). Dann legt man sich ein entsprechend
 * großes, leeres, transparentes Bild an und schreibt das Cursor-Icon dort oben rechts hinein. Den Hotspot
 * bilden wir aus Höhe und Breite des Icons. Wir haben für alle Fälle auch noch zwei verschieden große Bilder parat.
 * <p>
 * Der Tipp stammt im Kern aus https://stackoverflow.com/questions/2620188/how-to-set-custom-size-for-cursor-in-swing */
public class CursorFactory {
  public static Cursor createCursor(String baseImageName) {
    return createCursor(baseImageName, HotspotPlacement.Center);
  }

  public static Cursor createCursor(String baseImageName, HotspotPlacement hotspotPlacement) {
    Dimension bestCursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
    try {
      ImageIcon icon = IconReader.readImageIcon(baseImageName);
      if (icon.getIconWidth() > bestCursorSize.width || icon.getIconHeight() > bestCursorSize.height) {
        icon = IconReader.readImageIcon(baseImageName + "-32");
      }
      final BufferedImage bufferedImage = new BufferedImage( bestCursorSize.width, bestCursorSize.height, BufferedImage.TYPE_INT_ARGB );
      final Graphics graphic = bufferedImage.getGraphics();
      graphic.drawImage(icon.getImage(), 0, 0, null);

      Point hotSpot = hotspotPlacement.toPoint(icon.getIconWidth(), icon.getIconHeight());
      return Toolkit.getDefaultToolkit().createCustomCursor(bufferedImage, hotSpot, baseImageName);
    }
    catch(Exception x) {
      return new Cursor(Cursor.DEFAULT_CURSOR);
    }
  }

  public enum HotspotPlacement {
    Center, Bottom, Right, BottomRight;

    public Point toPoint(int iconWidth, int iconHeight) {
      int x = this == Right || this == BottomRight ? iconWidth - 1 : iconWidth / 2;
      int y = this == Bottom || this == BottomRight ? iconHeight - 1 : iconHeight / 2;
      return new Point(x, y);
    }
  }

}
