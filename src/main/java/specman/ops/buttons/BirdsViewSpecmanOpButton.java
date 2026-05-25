package specman.ops.buttons;

import specman.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class BirdsViewSpecmanOpButton extends AbstractSpecmanOpButton {

  public BirdsViewSpecmanOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    Image i = getBirdsViewImage();
    if (i == null) {
      return;
    }
    final int breite = i.getWidth(null);
    final int hoehe = i.getHeight(null);
    final JLabel l = new JLabel(new ImageIcon(i.getScaledInstance(breite / 5, hoehe / 5, Image.SCALE_SMOOTH)));
    final JPanel p = new JPanel(new BorderLayout());
    p.add(l, BorderLayout.CENTER);
    final JDialog d = new JDialog();
    d.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        float breitenFaktor = (float) p.getSize().width / breite;
        float hoehenFaktor = (float) p.getSize().height / hoehe;
        int skalierteBreite;
        int skalierteHoehe;
        if (breitenFaktor > hoehenFaktor) {
          skalierteHoehe = p.getSize().height;
          skalierteBreite = (int) (breite * hoehenFaktor);
        } else {
          skalierteBreite = p.getSize().width;
          skalierteHoehe = (int) (hoehe * breitenFaktor);
        }
        if (skalierteBreite > 0 && skalierteHoehe > 0) {
          l.setIcon(new ImageIcon(i.getScaledInstance(skalierteBreite, skalierteHoehe, Image.SCALE_SMOOTH)));
        }
      }
    });
    d.getContentPane().add(p);
    d.pack();
    d.setVisible(true);
  }

  private Image getBirdsViewImage() {
    if (getHauptSequenzContainer() == null) {
      return null;
    }
    Image image = createDiagramImage(
      getHauptSequenzContainer().getBounds().width,
      getHauptSequenzContainer().getBounds().height);
    getHauptSequenzContainer().paint(image.getGraphics());
    return image;
  }

}
