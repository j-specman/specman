package classman.render;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;

/** The way a host application gets the picture of a class diagram from its source text. What only the host knows,
 * the colors of its change sets and the words of its language, is given to the constructor. */
public final class DiagramRenderer {
  private final ChangeColors changeColors;
  private final String problemTitle;

  /** @param problemTitle headline of the picture shown instead of a diagram which can't be drawn */
  public DiagramRenderer(ChangeColors changeColors, String problemTitle) {
    this.changeColors = changeColors;
    this.problemTitle = problemTitle;
  }

  /** @return the scene of the diagram, or a picture of the problem if the source can't be drawn */
  public Scene render(String source) {
    try {
      return layout(new ClassDiagramParser_V001().parse(source));
    }
    catch (ClassDiagramParseException | IllegalArgumentException problem) {
      return new ProblemScene(new AwtTextMeasurer()).of(problemTitle, problem.getMessage());
    }
  }

  /** @throws IllegalArgumentException if the model refers to a class, attribute or relation which it doesn't have */
  public Scene layout(ClassDiagramModel_V001 model) {
    return new ClassDiagramLayouter(new AwtTextMeasurer(), changeColors).layout(model);
  }

  /** @return the color of the frame around a selected element which the change set has marked, as #rrggbb, or null
   * if the usual selection color can be told from the color of the change set */
  public String selectionColorOf(String changeSet) {
    return changeColors.selectionColorOf(changeSet);
  }

  /** @return the strong variant of the color of the change set as #rrggbb, for marks */
  public String strongColorOf(String changeSet) {
    return new ChangePalette(changeColors).strong(changeSet);
  }

  /** @return the pale variant of the color of the change set as #rrggbb, for backgrounds */
  public String paleColorOf(String changeSet) {
    return new ChangePalette(changeColors).pale(changeSet);
  }
}
