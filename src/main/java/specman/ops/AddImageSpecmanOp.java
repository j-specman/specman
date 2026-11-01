package specman.ops;

import specman.editarea.TextEditArea;
import specman.editarea.plantuml.PlantUmlImageEditArea;
import specman.i18n.IFileNameExtensionFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class AddImageSpecmanOp extends AbstractSpecmanOp {
  public static final String EMPTY_PLANTUML = "@startuml\n\n@enduml";

  public AddImageSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void addPlantUml() {
    TextEditArea lastFocused = getLastFocusedTextArea();
    if (lastFocused != null) {
      PlantUmlImageEditArea diagram = lastFocused.addPlantUmlImage(EMPTY_PLANTUML);
      // The diagram has to be laid out before the editor can be shown next to it
      SwingUtilities.invokeLater(diagram::startFirstEdit);
    }
  }

  public void addViaFileChooser() {
    TextEditArea lastFocused = getLastFocusedTextArea();
    if (lastFocused == null) {
      return;
    }
    try {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setCurrentDirectory(new File("."));
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.addChoosableFileFilter(new IFileNameExtensionFilter("msg.filter.images", "jpg", "png", "gif", "bmp"));
      fileChooser.setAcceptAllFileFilterUsed(true);
      if (fileChooser.showOpenDialog(getArbeitsbereich()) == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile != null && selectedFile.exists()) {
          BufferedImage image = ImageIO.read(selectedFile);
          lastFocused.addImage(image);
        }
      }
    }
    catch (IOException iox) {
      displayException(iox);
    }
  }

}
