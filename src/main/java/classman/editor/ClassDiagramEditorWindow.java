package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

/** The window in which a class diagram is edited. It is modal: While it is open the rest of the application can't
 * be operated, so nothing else changes the diagram. Esc and closing the window leave the editor, every operation has
 * already been reported to the host when it was completed. */
public class ClassDiagramEditorWindow extends JDialog {
  private static final int MAX_WIDTH = 1200;
  private static final int MAX_HEIGHT = 800;
  private static final int MIN_WIDTH = 500;
  private static final int MIN_HEIGHT = 350;
  private static final int FRAME_WIDTH_ALLOWANCE = 40;
  private static final int FRAME_HEIGHT_ALLOWANCE = 60;

  private final DiagramEditing editing;
  private final DiagramCanvas canvas;
  private final JScrollPane scrollPane;

  /** @throws ClassDiagramParseException if the source has a syntax error
   * @throws IllegalArgumentException if the diagram refers to an element which it doesn't have */
  public ClassDiagramEditorWindow(Component owner, String source, EditorHost host) throws ClassDiagramParseException {
    super(SwingUtilities.getWindowAncestor(owner), ModalityType.APPLICATION_MODAL);
    EditorTexts texts = new EditorTexts(host.locale());
    setTitle(texts.get("window.title"));
    editing = new DiagramEditing(source, host, texts);
    canvas = new DiagramCanvas(editing.scene());
    scrollPane = new JScrollPane(canvas);

    EditorPalette palette = new EditorPalette(texts, this::classDropped);
    getContentPane().add(palette, BorderLayout.WEST);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    Dimension preferred = canvas.getPreferredSize();
    setSize(Math.max(MIN_WIDTH, Math.min(preferred.width + palette.getPreferredSize().width + FRAME_WIDTH_ALLOWANCE,
        MAX_WIDTH)),
      Math.max(MIN_HEIGHT, Math.min(preferred.height + FRAME_HEIGHT_ALLOWANCE, MAX_HEIGHT)));
    getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW);
    bindKey(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, editing::undo);
    bindKey(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, editing::redo);
    bindKey(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, editing::redo);
    setLocationRelativeTo(owner);
  }

  /** Runs the operation on Ctrl+key and shows the diagram as it is afterwards. */
  private void bindKey(int keyCode, int modifiers, Runnable operation) {
    getRootPane().registerKeyboardAction(e -> {
      operation.run();
      canvas.setScene(editing.scene());
    }, KeyStroke.getKeyStroke(keyCode, modifiers), JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  private void classDropped(Point screenPoint) {
    JViewport viewport = scrollPane.getViewport();
    Point inViewport = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(inViewport, viewport);
    if (!viewport.contains(inViewport)) {
      return;
    }
    Point inCanvas = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(inCanvas, canvas);
    Point2D inDiagram = canvas.toDiagram(inCanvas);
    editing.addClass((int) Math.round(inDiagram.getX()), (int) Math.round(inDiagram.getY()));
    canvas.setScene(editing.scene());
  }
}
