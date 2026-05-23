package specman;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class AddImageSpecmanOp extends AbstractSpecmanOp {

  AddImageSpecmanOp(Specman specman) {
    super(specman);
  }

  void addViaFileChooser() {
    if (specman.lastFocusedTextArea == null) {
      return;
    }
    try {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setCurrentDirectory(new File("."));
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
      fileChooser.setAcceptAllFileFilterUsed(true);
      if (fileChooser.showOpenDialog(specman.arbeitsbereich) == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile != null && selectedFile.exists()) {
          BufferedImage image = ImageIO.read(selectedFile);
          specman.lastFocusedTextArea.addImage(image);
        }
      }
    }
    catch (IOException iox) {
      specman.displayException(iox);
    }
  }

}
