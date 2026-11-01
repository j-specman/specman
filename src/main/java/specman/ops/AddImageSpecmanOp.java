package specman.ops;

import specman.editarea.TextEditArea;
import specman.editarea.image.plantuml.PlantUmlFileChoice;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;
import specman.editarea.image.plantuml.PlantUmlSourceValidator;
import specman.editarea.image.AbstractSvgImageEditArea;
import specman.editarea.image.SvgNotDisplayableException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import static specman.i18n.Messages.i18n;


public class AddImageSpecmanOp extends AbstractSpecmanOp {
  public static final String EMPTY_PLANTUML = "@startuml\n\n@enduml";
  public static final String[] IMAGE_FILE_EXTENSIONS = {"jpg", "png", "gif", "bmp", "svg"};

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
      String source = readTextFile(file);
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

  /** The text with the line breaks Specman uses and without the byte order mark some editors put at the start. */
  private static String readTextFile(File file) throws IOException {
    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    return content
      .replace(BYTE_ORDER_MARK, "")
      .replace("\r\n", "\n")
      .replace('\r', '\n');
  }

  /** Asks the user for an image file and adds it after the text. Bitmaps are added as bitmaps, SVG files as
   * vector graphics. */
  public void addViaFileChooser() {
    TextEditArea lastFocused = getLastFocusedTextArea();
    if (lastFocused == null) {
      return;
    }
    File selectedFile = context.chooseImageFile();
    if (selectedFile != null && selectedFile.exists()) {
      try {
        if (isSvgFile(selectedFile)) {
          addSvg(lastFocused, selectedFile);
        }
        else {
          lastFocused.addImage(ImageIO.read(selectedFile));
        }
      }
      catch (IOException iox) {
        displayException(iox);
      }
    }
  }

  private static boolean isSvgFile(File file) {
    return file.getName().toLowerCase(Locale.ROOT).endsWith(".svg");
  }

  /** A file which can't be displayed is refused, so no graphic which fails to render ever gets into a spec. */
  private void addSvg(TextEditArea lastFocused, File file) throws IOException {
    String source = readTextFile(file);
    try {
      AbstractSvgImageEditArea.toIcon(source.getBytes(StandardCharsets.UTF_8));
    }
    catch (SvgNotDisplayableException notDisplayable) {
      String reason = notDisplayable.getMessage() != null ? notDisplayable.getMessage() : i18n("msg.svg.file.invalid.noreason");
      fehler("msg.svg.file.invalid", file.getName(), reason);
      return;
    }
    lastFocused.addSvgImage(source);
  }

}
