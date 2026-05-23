package specman;

import specman.view.AbstractSchrittView;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;

public class WelcomeMessagePanel extends JLabel {

  public WelcomeMessagePanel() {
    setText("<html>"
        + "<h2><i>Welcome to Specman " + SpecmanVersion.getVersion() + "</i></h2><i>"
        + "This is the modelling area. Click a button from the steps palette and "
        + "drag the mouse to the position where you want to place the step. Alternatively "
        + "just click the button from the steps palette to add a step below the one "
        + "focused by the text cursor."
        + "<p>"
        + "As soon as you have started your work you can change the diagramm width by "
        + "clicking on its right border and dragging it left or right."
        + "</i></html>");
    setOpaque(true);
    setForeground(Color.gray);
    setBackground(new Color(230, 230, 240));
    setBorder(new CompoundBorder(
        new LineBorder(DIAGRAMM_LINE_COLOR, AbstractSchrittView.LINIENBREITE),
        new EmptyBorder(15, 15, 15, 15)));
  }

}
