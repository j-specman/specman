package specman;

import specman.view.AbstractSchrittView;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.i18n.Messages.i18n;

public class WelcomeMessagePanel extends JLabel {

  public WelcomeMessagePanel() {
    setText(i18n("welcome.message", SpecmanVersion.getVersion()));
    setOpaque(true);
    setForeground(Color.gray);
    setBackground(new Color(230, 230, 240));
    setBorder(new CompoundBorder(
        new LineBorder(DIAGRAMM_LINE_COLOR, AbstractSchrittView.LINIENBREITE),
        new EmptyBorder(15, 15, 15, 15)));
  }

}
