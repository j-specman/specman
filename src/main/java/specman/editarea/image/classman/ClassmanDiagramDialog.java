package specman.editarea.image.classman;

import classman.render.Scene;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

/** A window which shows a Classman diagram painted with Graphics2D: the first step towards the diagram editor. */
public class ClassmanDiagramDialog extends JDialog {
  private static final int MAX_WIDTH = 1200;
  private static final int MAX_HEIGHT = 800;
  private static final int SCROLL_UNIT = 16;

  public ClassmanDiagramDialog(Component owner, Scene scene) {
    super(SwingUtilities.getWindowAncestor(owner), "Classman");
    setModalityType(ModalityType.MODELESS);
    ClassmanCanvas canvas = new ClassmanCanvas(scene);
    JScrollPane scrollPane = new JScrollPane(canvas);
    scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
    setContentPane(scrollPane);
    Dimension preferred = canvas.getPreferredSize();
    setSize(Math.min(preferred.width + 40, MAX_WIDTH), Math.min(preferred.height + 60, MAX_HEIGHT));
    getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW);
    setLocationRelativeTo(owner);
  }
}
