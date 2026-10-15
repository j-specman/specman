package specman.graphics;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IconReader {

  /** Decoding a PNG via ImageIO is expensive (it uses a temporary file as cache), and the same icons are needed
   * by every Specman instance, so the decoded images are shared. The ImageIcons are not, they are mutable. */
  private static final Map<String, Image> IMAGES = new ConcurrentHashMap<>();

  public static ImageIcon readImageIcon(String iconBasename) {
    return new ImageIcon(IMAGES.computeIfAbsent(iconBasename, IconReader::readImage));
  }

  private static Image readImage(String iconBasename) {
    String resource = "images/" + iconBasename + ".png";
    try {
      URL imageURL = IconReader.class.getClassLoader().getResource(resource);
      Image image = ImageIO.read(imageURL);
      if (image == null) {
        throw new IllegalArgumentException("Can't load image icon " + resource);
      }
      return image;
    }
    catch (IOException iox) {
      iox.printStackTrace();
      throw new IllegalArgumentException("Error reading image icon " + resource + ": " + iox.getMessage());
    }
  }

}
