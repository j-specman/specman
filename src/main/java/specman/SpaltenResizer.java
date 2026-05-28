package specman;

import specman.pdf.LineShape;
import specman.pdf.Shape;
import specman.undo.UndoableSpaltenbreiteAngepasst;

import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import static specman.view.AbstractSchrittView.LINIENBREITE;
import static specman.Specman.editor;

public class SpaltenResizer extends JPanel implements MouseListener {
	Integer dragX;
	boolean dragCancelled;
	final SpaltenContainerI container;
	final int spalte;
	static Cursor leftRightCursor;

	public SpaltenResizer(SpaltenContainerI container) {
		this(container, 0);
	}

	public SpaltenResizer(SpaltenContainerI container, int spalte) {
		createLeftRightCursor();
		this.container = container;
		this.spalte = spalte;
		setOpaque(false);
    addMouseListener(this);

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override public void mouseDragged(MouseEvent e) {
				if (!dragCancelled) {
					dragX = e.getX();
					editor().vertikalLinieSetzen(dragX, SpaltenResizer.this);
				}
			}
		});
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE && dragX != null) {
				dragX = null;
				dragCancelled = true;
				editor().vertikalLinieSetzen(0, null);
				return true;
			}
			return false;
		});
	}

	private Cursor createLeftRightCursor() {
		if (leftRightCursor == null) {
			leftRightCursor = CursorFactory.createCursor("left-right-cursor");
		}
		return leftRightCursor;
	}

  @Override public void mouseClicked(MouseEvent e) {}

  @Override public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {
      dragCancelled = false;
      if (dragX != null) {
        int ermoeglichteVeraenderung = container.spaltenbreitenAnpassenNachMausDragging(e.getX(), spalte);
        if (ermoeglichteVeraenderung != 0) {
          editor().addEdit(new UndoableSpaltenbreiteAngepasst(container, ermoeglichteVeraenderung, spalte));
        }
        dragX = null;
        editor().vertikalLinieSetzen(0, null);
      }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    editor().setCursor(leftRightCursor);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    editor().setCursor(Cursor.getDefaultCursor());
  }

  public Shape getShape() {
		return new LineShape(getX(), getY() + LINIENBREITE, getX(), getY() + getHeight() - LINIENBREITE);
	}
}