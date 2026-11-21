package classman.editor;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** Makes a palette button a drag source: while the mouse is dragged, a small window with the icon and label of the
 * button follows it, and where the mouse is released, the screen point is reported. A click without a drag creates
 * nothing. */
class PaletteDragAdapter extends MouseAdapter {
  private static final int DRAG_THRESHOLD = 6;
  private static final int GHOST_OFFSET = 8;

  private final String label;
  private final Icon icon;
  private final Consumer<Point> dropped;
  private Point pressedAt;
  private JWindow ghost;

  PaletteDragAdapter(String label, Icon icon, Consumer<Point> dropped) {
    this.label = label;
    this.icon = icon;
    this.dropped = dropped;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    pressedAt = e.getLocationOnScreen();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    Point at = e.getLocationOnScreen();
    if (ghost == null && pressedAt.distance(at) < DRAG_THRESHOLD) {
      return;
    }
    if (ghost == null) {
      ghost = newGhost((AbstractButton) e.getComponent());
    }
    ghost.setLocation(at.x + GHOST_OFFSET, at.y + GHOST_OFFSET);
    ghost.setVisible(true);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (ghost == null) {
      return;
    }
    ghost.dispose();
    ghost = null;
    dropped.accept(e.getLocationOnScreen());
  }

  private JWindow newGhost(AbstractButton button) {
    JWindow window = new JWindow(SwingUtilities.getWindowAncestor(button));
    JLabel content = new JLabel(label, icon, JLabel.LEADING);
    content.setOpaque(true);
    content.setBackground(UIManager.getColor("Panel.background"));
    content.setBorder(new LineBorder(Color.GRAY));
    window.setFocusableWindowState(false);
    window.add(content);
    window.pack();
    return window;
  }
}
