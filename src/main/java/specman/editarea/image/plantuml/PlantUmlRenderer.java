package specman.editarea.image.plantuml;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileSystem;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.security.SFile;
import specman.EditorI;
import specman.graphics.Styles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import static specman.Specman.editor;

/** Renders PlantUML source to SVG with Specman's fixed layout engine (ELK) and its shipped fonts. */
public final class PlantUmlRenderer {
  // Touching Styles registers Specman's shipped fonts with AWT, so PlantUML measures text with them and the
  // SVG renderer finds them by name. labelFont is Roboto, the shipped sans-serif font.
  private static final String PRELUDE = "\n!pragma layout elk"
      + "\nskinparam defaultFontName \"" + Styles.labelFont.getFamily() + "\""
      + "\nskinparam monospacedFontName \"" + Styles.MONOSPACEFONT.getFamily() + "\"";

  private PlantUmlRenderer() {}

  /** Syntax errors do not throw - PlantUML renders them as an error graphic. */
  public static byte[] renderSvg(String plantUmlSource) {
    String withPrelude = plantUmlSource.replaceFirst("@startuml[^\\n]*", "$0" + PRELUDE);
    ByteArrayOutputStream svg = new ByteArrayOutputStream();
    useModelFileDirectory();
    try {
      new SourceStringReader(withPrelude).outputImage(svg, new FileFormatOption(FileFormat.SVG));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return svg.toByteArray();
  }

  static void useModelFileDirectory() {
    EditorI editor = editor();
    File modelFile = editor != null ? editor.getDiagrammDatei() : null;
    File directory = modelFile != null ? modelFile.getAbsoluteFile().getParentFile() : null;
    FileSystem.getInstance().setCurrentDir(directory != null ? new SFile(directory.getPath()) : null);
  }

}
