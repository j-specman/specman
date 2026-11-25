package classman.render;

import java.util.ArrayList;
import java.util.List;

/** The picture shown instead of a diagram which can't be drawn: a red-framed box with the title and the problem. */
class ProblemScene {
  private static final String FILL = "#ffe0e0";
  private static final String STROKE = "#c00000";
  private static final double PADDING = 8;
  private static final double TEXT_WIDTH = 420;

  private final TextMeasurer measurer;

  ProblemScene(TextMeasurer measurer) {
    this.measurer = measurer;
  }

  Scene of(String title, String problem) {
    List<String> lines = new TextWrapper(measurer).wrap(title + "\n" + problem, TEXT_WIDTH, TEXT_WIDTH, false);
    double width = TEXT_WIDTH + 2 * PADDING;
    double height = lines.size() * measurer.lineHeight() + 2 * PADDING;
    List<AbstractSceneElement> elements = new ArrayList<>();
    elements.add(new SceneRectangle(0, 0, width, height, FILL, STROKE));
    double baseline = PADDING + measurer.ascent();
    for (String line : lines) {
      elements.add(new SceneText(PADDING, baseline, line, false, TextAnchor.START, measurer.width(line, false)));
      baseline += measurer.lineHeight();
    }
    return new Scene(elements, 0, 0, width, height);
  }
}
