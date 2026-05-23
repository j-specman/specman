package specman.graphics;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class IconReader {

  public static ImageIcon readImageIcon(String iconBasename) {
    String resource = "images/" + iconBasename + ".png";
    try {
      URL imageURL = IconReader.class.getClassLoader().getResource(resource);
      Image image = ImageIO.read(imageURL);
      if (image == null) {
        throw new IllegalArgumentException("Can't load image icon " + resource);
      }
      return new ImageIcon(image);
    }
    catch (IOException iox) {
      iox.printStackTrace();
      throw new IllegalArgumentException("Error reading image icon " + resource + ": " + iox.getMessage());
    }
  }

}
