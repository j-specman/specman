package specman.editarea.image.classman;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.AbstractSceneElement;
import classman.render.AwtTextMeasurer;
import classman.render.ClassDiagramLayouter;
import classman.render.Scene;
import classman.render.SceneRectangle;
import classman.render.SceneSvgWriter;
import classman.render.SceneText;
import classman.render.TextAnchor;
import classman.render.TextMeasurer;
import classman.render.TextWrapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static specman.i18n.Messages.i18n;

/** Renders the source of a Classman class diagram to SVG with the shipped fonts. */
public final class ClassmanRenderer {
  private static final String ERROR_FILL = "#ffe0e0";
  private static final String ERROR_STROKE = "#c00000";
  private static final double ERROR_PADDING = 8;
  private static final double ERROR_TEXT_WIDTH = 420;

  private ClassmanRenderer() {}

  /** @return the diagram as SVG, or a picture of the problem if the source can't be drawn, so that a file with a
   * source which was edited by hand can still be loaded */
  public static byte[] renderSvg(String source) {
    SceneSvgWriter writer = new SceneSvgWriter();
    try {
      return writer.write(sceneOf(modelOf(source))).getBytes(StandardCharsets.UTF_8);
    }
    catch (ClassDiagramParseException | IllegalArgumentException problem) {
      return writer.write(errorScene(problem.getMessage())).getBytes(StandardCharsets.UTF_8);
    }
  }

  /** @throws ClassDiagramParseException if the source has a syntax error */
  static ClassDiagramModel_V001 modelOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source);
  }

  /** @throws IllegalArgumentException if the model refers to a class, attribute or relation which it doesn't have */
  static Scene sceneOf(ClassDiagramModel_V001 model) {
    return new ClassDiagramLayouter(new AwtTextMeasurer()).layout(model);
  }

  private static Scene errorScene(String problem) {
    TextMeasurer measurer = new AwtTextMeasurer();
    List<String> lines = new ArrayList<>(new TextWrapper(measurer).wrap(
      i18n("msg.classman.render.error") + "\n" + problem, ERROR_TEXT_WIDTH, ERROR_TEXT_WIDTH, false));
    List<AbstractSceneElement> elements = new ArrayList<>();
    double width = ERROR_TEXT_WIDTH + 2 * ERROR_PADDING;
    double height = lines.size() * measurer.lineHeight() + 2 * ERROR_PADDING;
    elements.add(new SceneRectangle(0, 0, width, height, ERROR_FILL, ERROR_STROKE));
    double baseline = ERROR_PADDING + measurer.ascent();
    for (String line : lines) {
      elements.add(new SceneText(ERROR_PADDING, baseline, line, false, TextAnchor.START, measurer.width(line, false)));
      baseline += measurer.lineHeight();
    }
    return new Scene(elements, 0, 0, width, height);
  }
}
