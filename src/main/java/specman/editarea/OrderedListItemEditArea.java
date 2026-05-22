package specman.editarea;

import specman.Aenderungsart;
import specman.Specman;
import specman.model.v001.ListItemEditAreaModel_V001;
import specman.pdf.LabelShapeText;
import specman.pdf.Shape;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;

public class OrderedListItemEditArea extends AbstractListItemEditArea {
  private static final int DEFAULT_RIGHT_GAP = 3;
  private int rightGap;

  public OrderedListItemEditArea(TextEditArea initialContent, Aenderungsart aenderungsart) {
    super(initialContent, aenderungsart);
    initRightGap();
  }

  private void initRightGap() {
    rightGap = DEFAULT_RIGHT_GAP * Specman.instance().getZoomFactor() / 100;
  }

  public OrderedListItemEditArea(ListItemEditAreaModel_V001 model) {
    super(model);
  }

  @Override
  protected void drawPrompt(Graphics2D g) {
    int baseline = content.getBaseline();
    String itemNumberString = Integer.toString(getParent().getItemNumber(this));
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    g.setFont(content.getFont());
    g.setColor(DIAGRAMM_LINE_COLOR);
    g.drawString(itemNumberString, promptPosition(itemNumberString), baseline);
  }

  private int promptPosition(String itemNumberString) {
    Font font = content.getFont();
    FontMetrics metrics = getFontMetrics(font);
    int promptWidth = metrics.stringWidth(itemNumberString);
    return promptSpace - promptWidth - rightGap;
  }

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    super.skalieren(prozentNeu, prozentAktuell);
    initRightGap();
  }

  @Override
  public specman.pdf.Shape getShape() {
    int topMargin = content.getTopMargin();
    String itemNumberString = Integer.toString(getParent().getItemNumber(this));

    return super.getShape()
      .add(new Shape(promptPosition(itemNumberString), topMargin)
        .withText(new LabelShapeText(itemNumberString, new Insets(0, 0, 0, 0), DIAGRAMM_LINE_COLOR, content.getFont()))
      );
  }

  @Override
  protected AbstractListItemEditArea createSplittedItem(TextEditArea splitTextEditArea) {
    return new OrderedListItemEditArea(splitTextEditArea, aenderungsart);
  }

  @Override
  protected boolean ordered() { return true; }

  @Override
  public boolean isOrderedListItemArea() { return true; }
}
