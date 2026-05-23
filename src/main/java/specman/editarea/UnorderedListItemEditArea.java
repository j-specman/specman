package specman.editarea;

import specman.Aenderungsart;
import specman.Specman;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.ListItemEditAreaModel_V001;
import specman.pdf.CircleShape;

import java.awt.*;
import java.awt.geom.Ellipse2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;

public class UnorderedListItemEditArea extends AbstractListItemEditArea {
  static final int DEFAULT_PROMPT_RADIUS = 3;
  private int promptRadius;

  public UnorderedListItemEditArea(TextEditArea initialContent, Aenderungsart aenderungsart) {
    super(initialContent, aenderungsart);
  }

  public UnorderedListItemEditArea(ListItemEditAreaModel_V001 model) {
    super(model);
  }

  @Override
  protected void initLayout() {
    super.initLayout();
    initPromptRadius();
  }

  @Override
  protected void drawPrompt(Graphics2D g) {
    Point prompCenter = promptCenter();
    Shape circle = new Ellipse2D.Double(prompCenter.x - promptRadius, prompCenter.y - promptRadius, 2.0 * promptRadius, 2.0 * promptRadius);
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    g.setColor(DIAGRAMM_LINE_COLOR);
    g.fill(circle);
  }

  private Point promptCenter() {
    Integer firstLineHeight = content.getFirstLineHeight();
    if (firstLineHeight == null) {
      firstLineHeight = promptSpace;
    }
    return new Point(
      promptSpace / 2 + DEFAULT_PROMPT_RADIUS,
      firstLineHeight / 2 + DEFAULT_PROMPT_RADIUS
    );
  }

  @Override
  public void addSchrittnummer(StepnumberLabel schrittNummer) {
  }

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    super.skalieren(prozentNeu, prozentAktuell);
    initPromptRadius();
  }

  private void initPromptRadius() {
    promptRadius = DEFAULT_PROMPT_RADIUS * Specman.instance().getZoomFactor() / 100;
  }

  @Override
  public specman.pdf.Shape getShape() {
    return super.getShape()
      .add(new CircleShape(promptCenter(), promptRadius));
  }

  @Override
  protected AbstractListItemEditArea createSplittedItem(TextEditArea splitTextEditArea) {
    return new UnorderedListItemEditArea(splitTextEditArea, aenderungsart);
  }

  @Override
  protected boolean ordered() { return false; }

}
