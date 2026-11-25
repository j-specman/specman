package specman.ops;

import specman.editarea.TextEditArea;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.ForeignStereotypes;
import specman.editarea.image.classman.ClassmanSourceValidator;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.plantuml.PlantUmlImageEditArea;
import specman.editarea.image.plantuml.PlantUmlSourceValidator;
import specman.editarea.image.SvgPicture;
import specman.editarea.image.SvgNotDisplayableException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import static specman.i18n.Messages.i18n;


public class AddImageSpecmanOp extends AbstractSpecmanOp {
  public static final String EMPTY_PLANTUML = "@startuml\n\n@enduml";
  public static final String EMPTY_CLASSMAN = "classdiagram {\n\n}";
  public static final String[] IMAGE_FILE_EXTENSIONS = {"jpg", "png", "gif", "bmp", "svg", ClassmanImageEditArea.FILE_EXTENSION};

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
    SourceFileChoice choice = context.chooseSourceFile(PlantUmlImageEditArea.FILE_KIND, true);
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
    SourceFileChoice choice = context.chooseSourceFile(PlantUmlImageEditArea.FILE_KIND, false);
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
   * vector graphics, and the source of a Classman class diagram as a diagram. */
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
        else if (isClassmanFile(selectedFile)) {
          addClassman(lastFocused, selectedFile);
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

  private static boolean isClassmanFile(File file) {
    return file.getName().toLowerCase(Locale.ROOT).endsWith("." + ClassmanImageEditArea.FILE_EXTENSION);
  }

  /** Asks the user for a file to take the diagram's source from. Without a file, the diagram starts empty and the user
   * can enter its content right away. A file with a source which can't be drawn is refused, so no diagram which fails to
   * render ever gets into a spec. */
  public void addClassman() {
    TextEditArea lastFocused = getLastFocusedTextArea();
    if (lastFocused == null) {
      return;
    }
    SourceFileChoice choice = context.chooseSourceFile(ClassmanImageEditArea.FILE_KIND, true);
    if (choice.isEmptyDiagram()) {
      addEmptyClassman(lastFocused);
    }
    else if (!choice.isCancelled()) {
      addClassman(lastFocused, choice.file());
    }
  }

  private void addEmptyClassman(TextEditArea lastFocused) {
    ClassmanImageEditArea diagram = lastFocused.addClassmanImage(EMPTY_CLASSMAN);
    // The diagram has to be laid out before the editor can be shown next to it
    SwingUtilities.invokeLater(diagram::startFirstEdit);
  }

  /** A file with a source which can't be drawn is refused, so no diagram which fails to render ever gets into a spec. */
  private void addClassman(TextEditArea lastFocused, File file) {
    String source = readValidClassmanSource(file);
    if (source != null) {
      lastFocused.addClassmanImage(source);
    }
  }

  /** Asks the user for a file and replaces the source of the diagram by its content, which is subject to the same
   * rules as for a new diagram. In change mode, the replacement is a change like any other source change. */
  public void importClassman(ClassmanImageEditArea diagram) {
    SourceFileChoice choice = context.chooseSourceFile(ClassmanImageEditArea.FILE_KIND, false);
    if (!choice.isCancelled()) {
      String source = readValidClassmanSource(choice.file());
      if (source != null) {
        diagram.changeSource(source);
      }
    }
  }

  /** @return the source in the file, or null if the user got told why it can't be used */
  private String readValidClassmanSource(File file) {
    try {
      String source = readTextFile(file);
      String problem = new ClassmanSourceValidator().problemOf(source);
      if (problem != null) {
        fehler("msg.classman.file.invalid", file.getName(), problem);
        return null;
      }
      Set<String> dropped = new LinkedHashSet<>();
      String cleaned = ForeignStereotypes.withoutUnknownConfigs(source, dropped);
      toastSkippedMetaTags(dropped);
      return cleaned;
    }
    catch (IOException iox) {
      displayException(iox);
      return null;
    }
  }

  /** A file which can't be displayed is refused, so no graphic which fails to render ever gets into a spec. */
  private void addSvg(TextEditArea lastFocused, File file) throws IOException {
    String source = readTextFile(file);
    try {
      new SvgPicture(source.getBytes(StandardCharsets.UTF_8));
    }
    catch (SvgNotDisplayableException notDisplayable) {
      String reason = notDisplayable.getMessage() != null ? notDisplayable.getMessage() : i18n("msg.svg.file.invalid.noreason");
      fehler("msg.svg.file.invalid", file.getName(), reason);
      return;
    }
    lastFocused.addSvgImage(source);
  }

}
