package classman.render;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.StereotypeReference_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;

/** The way a host application gets the picture of a class diagram from its source text. What only the host knows,
 * the colors of its change sets, what the stereotypes look like which the diagram refers to, and the words of its
 * language, is given to the constructor. */
public final class DiagramRenderer {
  private final ChangeColors changeColors;
  private final StereotypeResolver stereotypes;
  private final String problemTitle;

  /** @param stereotypes tells what the stereotypes look like which refer to a configuration of the host
   * @param problemTitle headline of the picture shown instead of a diagram which can't be drawn */
  public DiagramRenderer(ChangeColors changeColors, StereotypeResolver stereotypes, String problemTitle) {
    this.changeColors = changeColors;
    this.stereotypes = stereotypes;
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

  /** @param link the link of a diagram whose file the host couldn't use
   * @param problem what is wrong, in the language of the user
   * @return the picture to show instead of the diagram */
  public Scene renderLinkPlaceholder(String link, String problem) {
    return new LinkPlaceholderScene(new AwtTextMeasurer()).of(link, problem);
  }

  /** @throws IllegalArgumentException if the model refers to a class, attribute or relation which it doesn't have */
  public Scene layout(ClassDiagramModel_V001 model) {
    return new ClassDiagramLayouter(new AwtTextMeasurer(), changeColors, stereotypes).layout(model);
  }

  /** @return what the host says about the configuration which the stereotype refers to, null if the stereotype is a
   * free text or the host doesn't know the configuration */
  public StereotypeLook stereotypeLookOf(String stereotype) {
    if (!StereotypeReference_V001.isReference(stereotype)) {
      return null;
    }
    return stereotypes.lookOf(StereotypeReference_V001.nameOf(stereotype));
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
