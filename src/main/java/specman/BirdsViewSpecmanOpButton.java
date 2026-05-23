package specman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class BirdsViewSpecmanOpButton extends AbstractSpecmanOpButton {

  BirdsViewSpecmanOpButton(Specman specman) {
    super(specman);
  }

  @Override
  void execute() throws EditException {
    if (specman.hauptSequenzContainer == null) {
      return;
    }
    final int breite = specman.hauptSequenzContainer.getBounds().width;
    final int hoehe = specman.hauptSequenzContainer.getBounds().height;
    final Image i = specman.createImage(breite, hoehe);
    specman.hauptSequenzContainer.paint(i.getGraphics());
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

}
