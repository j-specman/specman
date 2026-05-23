package specman;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.view.AbstractSchrittView;

import javax.swing.*;
import java.awt.*;

public class WorkingAreaPanel extends JPanel {
  public static final int INITIAL_DIAGRAMM_WIDTH = 700;

  private static final BasicStroke GESTRICHELTE_LINIE =
    new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1.0f, new float[] {10.0f, 10.0f }, 0f);

  private final FormLayout hauptlayout;
  private Integer dragX;
  private JComponent welcomeMessage;

  public WorkingAreaPanel() {
    hauptlayout = new FormLayout(
      "20px, " + INITIAL_DIAGRAMM_WIDTH + "px, " + AbstractSchrittView.FORMLAYOUT_GAP,
      "10px, fill:pref, fill:default, fill:pref");
    setLayout(hauptlayout);
    setBackground(new Color(247, 247, 253));
    showWelcomeMessage();
  }

  public void showWelcomeMessage() {
    welcomeMessage = new WelcomeMessagePanel();
    add(welcomeMessage, CC.xy(2, 3));
  }

  public boolean dropWelcomeMessage() {
    if (welcomeMessage == null) {
      return false;
    }
    remove(welcomeMessage);
    welcomeMessage = null;
    return true;
  }

  public void diagrammbreiteSetzen(int breite) {
    hauptlayout.setColumnSpec(2, ColumnSpec.decode(breite + "px"));
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (dragX != null) {
      Graphics2D g2 = (Graphics2D)g;
      g2.setStroke(GESTRICHELTE_LINIE);
      g.drawLine(dragX, 0, dragX, getHeight());
    }
  }

  public void showDragLine(Integer dragX) {
    this.dragX = dragX;
    repaint();
  }
}
