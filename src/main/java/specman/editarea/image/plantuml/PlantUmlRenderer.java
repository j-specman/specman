package specman.editarea.image.plantuml;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileSystem;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;
import net.sourceforge.plantuml.security.SFile;
import specman.EditorI;
import specman.graphics.Styles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Logger;

import static specman.Specman.editor;

/** Renders PlantUML source to SVG with Specman's shipped fonts. The layout engine is ELK, which needs nothing
 * installed. If a Graphviz dot is installed on the machine (see {@link DotLocator}), it is used instead:
 * its layout can be influenced from the source, e.g. by "left to right direction", which ELK in PlantUML
 * cannot. The minimal dot PlantUML carries along is deliberately never used. */
public final class PlantUmlRenderer {
  private static final Logger LOG = Logger.getLogger(PlantUmlRenderer.class.getName());
  private static final File INSTALLED_DOT = DotLocator.forThisMachine().locate();
  // Touching Styles registers Specman's shipped fonts with AWT, so PlantUML measures text with them and the
  // SVG renderer finds them by name. labelFont is Roboto, the shipped sans-serif font.
  private static final String PRELUDE = (INSTALLED_DOT == null ? "\n!pragma layout elk" : "")
      + "\nskinparam defaultFontName \"" + Styles.labelFont.getFamily() + "\""
      + "\nskinparam monospacedFontName \"" + Styles.MONOSPACEFONT.getFamily() + "\"";

  static {
    if (INSTALLED_DOT != null) {
      GraphvizRuntimeEnvironment.getInstance().setDotExecutable(INSTALLED_DOT.getAbsolutePath());
      LOG.info("PlantUML layout engine: Graphviz, " + INSTALLED_DOT);
    }
    else {
      LOG.info("PlantUML layout engine: ELK, no working dot found via " + DotLocator.ENVIRONMENT_VARIABLE
        + " (" + System.getenv(DotLocator.ENVIRONMENT_VARIABLE) + ") or PATH (" + System.getenv("PATH") + ")");
    }
  }

  private PlantUmlRenderer() {}

  /** @return whether the diagrams are laid out by an installed Graphviz and not by ELK */
  public static boolean usesGraphviz() {
    return INSTALLED_DOT != null;
  }

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
