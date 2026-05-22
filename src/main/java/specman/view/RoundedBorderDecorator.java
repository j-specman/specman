package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.Specman;
import specman.styles.Styles;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import static specman.styles.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;

public class RoundedBorderDecorator extends JPanel {
  private static final int INSET = 15;
  private static final int INNER_BORDERLINE_WIDTH = 2;
  private static final int ARC_SIZE = 25;
  private static final double STEPNUMBER_BACKGROUND_WIDTH = 25; // Long enough so the background fills the corner

  private final JComponent decoratedComponent;
  private final FormLayout layout;
  private RoundedBorderDecorationStyle style;
  private int inset;
  private int arc;
  private double stepnumberTextheight;

  public RoundedBorderDecorator(JComponent componentToDecorate, double stepnumberTextheight) {
    inset = INSET;
    arc = ARC_SIZE;
    setBackground(BACKGROUND_COLOR_STANDARD);
    String commonInsetSpec = (inset + INNER_BORDERLINE_WIDTH) + "px";
    layout = new FormLayout("0px,10px:grow,0px", "0px,fill:pref:grow,0px");
    setLayout(layout);
    add(componentToDecorate, CC.xy(2, 2));
    decoratedComponent = componentToDecorate;
    style = RoundedBorderDecorationStyle.Co;
    skalieren(Specman.instance().getZoomFactor(), stepnumberTextheight);
  }

  public RoundedBorderDecorationStyle getStyle() { return style; }

  public void setStyle(RoundedBorderDecorationStyle style) {
    this.style = style;
    updateTopInset();
  }

  private void updateTopInset() {
    int topInset = style.withTopInset() ? inset + INNER_BORDERLINE_WIDTH : INNER_BORDERLINE_WIDTH;
    layout.setRowSpec(1, RowSpec.decode(topInset + "px"));
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    drawDecoration(g);
  }

  void drawDecoration(Graphics g) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D) g;
      Color originalColor = g2d.getColor();

      antialiasingOn(g2d);
      Shape innerShape = createInnerShape();
      drawNumberStepBackground(g2d, innerShape);
      drawOuterBorderArea(g2d, innerShape);
      drawInnerBorderLine(g2d, innerShape);

      g2d.setColor(originalColor);
    }
  }

  private void antialiasingOn(Graphics2D g2d) {
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHints(rh);
  }

  public RoundRectangle2D.Float createInnerShape() {
    int borderX = inset + (INNER_BORDERLINE_WIDTH / 2);
    int borderY = style.withTopInset() ? borderX : INNER_BORDERLINE_WIDTH / 2;
    int borderWidthMinus = borderX * 2;
    int borderHeightMinus = borderX + borderY;

    return new RoundRectangle2D.Float(
        borderX,
        borderY,
        (float)(getWidth() - borderWidthMinus),
        (float)(getHeight() - borderHeightMinus),
        arc, arc);
  }

  /**
   * Draws a rectangle to fill the space between the stepNumber and innerBorder
   * TODO Rework magic numbers in x and y
   */
  private void drawNumberStepBackground(Graphics2D g2d, Shape innerShape) {
    g2d.setColor(Styles.SCHRITTNUMMER_FARBE.color);

    double x = (innerShape.getBounds().getWidth() + Specman.instance().scale(INNER_BORDERLINE_WIDTH * 2));
    double y = (innerShape.getBounds().getY() + Specman.instance().scale(1.5));
    Rectangle2D.Double background = new Rectangle2D.Double(x, y, STEPNUMBER_BACKGROUND_WIDTH, stepnumberTextheight);
    g2d.fill(background);
  }

  private void drawInnerBorderLine(Graphics2D g2d, Shape innerShape) {
    g2d.setColor(DIAGRAMM_LINE_COLOR);
    g2d.setStroke(new BasicStroke(2));
    g2d.draw(innerShape);
  }

  private void drawOuterBorderArea(Graphics2D g2d, Shape innerShape) {
    g2d.setColor(BACKGROUND_COLOR_STANDARD);
    Shape outer = new Rectangle2D.Float(0, 0, getWidth(), getHeight());
    Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    path.append(outer, false);
    path.append(innerShape, false);
    g2d.fill(path);
  }

  public JComponent getDecoratedComponent() { return decoratedComponent; }

  public void skalieren(int percent, double stepnumberTextheight) {
    this.stepnumberTextheight = stepnumberTextheight;
    inset = (INSET * percent) / 100;
    arc = (ARC_SIZE * percent) / 100;
    updateTopInset();
    String otherInsetSpec = (inset + INNER_BORDERLINE_WIDTH) + "px";
    ColumnSpec columnSpec = ColumnSpec.decode(otherInsetSpec);
    layout.setColumnSpec(1, columnSpec);
    layout.setColumnSpec(3, columnSpec);
    layout.setRowSpec(3, RowSpec.decode(otherInsetSpec));
  }

  public int getArc() { return arc; }
}