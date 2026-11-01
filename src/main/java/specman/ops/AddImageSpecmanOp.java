package specman.ops;

import specman.editarea.TextEditArea;
import specman.editarea.plantuml.PlantUmlFileChoice;
import specman.editarea.plantuml.PlantUmlImageEditArea;
import specman.editarea.plantuml.PlantUmlSourceValidator;
import specman.i18n.IFileNameExtensionFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


public class AddImageSpecmanOp extends AbstractSpecmanOp {
  public static final String EMPTY_PLANTUML = "@startuml\n\n@enduml";

  public AddImageSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  /** Asks the user for a file to take the diagram's source from. Without a file, the diagram starts empty. A file
   * with content PlantUML source must not have is refused, so no diagram with an invalid source ever gets into a spec. */
  public void addPlantUml() {
    TextEditArea lastFocused = getLastFocusedTextArea();
    if (lastFocused == null) {
      return;
    }
    PlantUmlFileChoice choice = context.choosePlantUmlFile(true);
    if (choice.isEmptyDiagram()) {
      addEmptyPlantUml(lastFocused);
    }
    else if (!choice.isCancelled()) {
      String source = readValidPlantUmlSource(choice.file());
      if (source != null) {
        lastFocused.addPlantUmlImage(source);
      }
    }
  }

  private void addEmptyPlantUml(TextEditArea lastFocused) {
    PlantUmlImageEditArea diagram = lastFocused.addPlantUmlImage(EMPTY_PLANTUML);
    // The diagram has to be laid out before the editor can be shown next to it
    SwingUtilities.invokeLater(diagram::startFirstEdit);
  }

  /** Asks the user for a file and replaces the source of the diagram by its content, which is subject to the same
   * rules as in {@link #addPlantUml()}. In change mode, the replacement is a change like any other source change. */
  public void importPlantUml(PlantUmlImageEditArea diagram) {
    PlantUmlFileChoice choice = context.choosePlantUmlFile(false);
    if (!choice.isCancelled()) {
      String source = readValidPlantUmlSource(choice.file());
      if (source != null) {
        diagram.changeSource(source);
      }
    }
  }

  /** @return the source in the file, or null if the user got told why it can't be used */
  private String readValidPlantUmlSource(File file) {
    try {
      String source = readPlantUmlSource(file);
      String problem = new PlantUmlSourceValidator().problemOf(source);
      if (problem != null) {
        fehler("msg.plantuml.file.invalid", file.getName(), problem);
        return null;
      }
      return source;
    }
    catch (IOException iox) {
      displayException(iox);
      return null;
    }
  }

  private static final String BYTE_ORDER_MARK = String.valueOf((char) 0xFEFF);

  /** The source with the line breaks Specman uses and without the byte order mark some editors put at the start. */
  private static String readPlantUmlSource(File file) throws IOException {
    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    return content
      .replace(BYTE_ORDER_MARK, "")
      .replace("\r\n", "\n")
      .replace('\r', '\n');
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
