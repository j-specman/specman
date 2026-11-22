package classman.editor;

import classman.model.v001.ClassKind_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.render.SceneTextArea;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.function.BiConsumer;
import java.awt.geom.Rectangle2D;

/** The window in which a class diagram is edited. It is modal: While it is open the rest of the application can't
 * be operated, so nothing else changes the diagram. Esc and closing the window leave the editor, every operation has
 * already been reported to the host when it was completed. */
public class ClassDiagramEditorWindow extends JDialog {
  private static final int MIN_WIDTH = 900;
  private static final int MIN_HEIGHT = 650;

  private final DiagramEditing editing;
  private final EditorTexts texts;
  private final DiagramCanvas canvas;
  private final JScrollPane scrollPane;
  private final TextEditor textEditor;
  private final EditorPalette palette;

  /** @throws ClassDiagramParseException if the source has a syntax error
   * @throws IllegalArgumentException if the diagram refers to an element which it doesn't have */
  public ClassDiagramEditorWindow(Component owner, String source, EditorHost host) throws ClassDiagramParseException {
    super(SwingUtilities.getWindowAncestor(owner), ModalityType.APPLICATION_MODAL);
    texts = new EditorTexts(host.locale());
    setTitle(texts.get("window.title"));
    editing = new DiagramEditing(source, host);
    canvas = new DiagramCanvas(editing.scene());
    textEditor = new TextEditor(canvas, editing, this::showDiagram);
    canvas.setPointerListener(new PointerControl(editing, this::showDiagram, textEditor::start, this::showContextMenu,
      canvas::toDiagramLength));
    scrollPane = new JScrollPane(canvas);

    palette = new EditorPalette(texts, this::classDropped,
      screenPoint -> boxDropped(screenPoint, textEditor::startNewNote), this::relationToolToggled,
      this::relationTypeChosen, this::anchorToolToggled, this::assocClassToolToggled);
    getContentPane().add(palette, BorderLayout.WEST);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    pack();
    setSize(Math.max(MIN_WIDTH, getWidth()), Math.max(MIN_HEIGHT, getHeight()));
    getRootPane().registerKeyboardAction(e -> escape(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW);
    bindKey(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, editing::undo);
    bindKey(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, editing::redo);
    bindKey(KeyEvent.VK_DELETE, 0, editing::deleteSelected);
    bindKey(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, editing::redo);
    placeNear(owner);
  }

  /** Opens near the visible part of the diagram, but completely within the usable part of its screen. */
  private void placeNear(Component owner) {
    GraphicsConfiguration screen = owner.getGraphicsConfiguration() != null
      ? owner.getGraphicsConfiguration()
      : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    Insets taskBarAndSuch = Toolkit.getDefaultToolkit().getScreenInsets(screen);
    Rectangle usable = screen.getBounds();
    usable.x += taskBarAndSuch.left;
    usable.y += taskBarAndSuch.top;
    usable.width -= taskBarAndSuch.left + taskBarAndSuch.right;
    usable.height -= taskBarAndSuch.top + taskBarAndSuch.bottom;
    setBounds(WindowPlacement.of(withRoomForScrollBars(getSize(), usable), screenRectOfVisiblePart(owner, usable),
      usable));
  }

  /** The window is sized for the whole diagram. Where that is more than the screen has, the diagram scrolls, and the
   * scroll bar takes room from the other direction, which would make a second scroll bar necessary. */
  private Dimension withRoomForScrollBars(Dimension wanted, Rectangle usable) {
    int width = wanted.width;
    int height = wanted.height;
    if (wanted.height > usable.height) {
      width += scrollPane.getVerticalScrollBar().getPreferredSize().width;
    }
    if (wanted.width > usable.width) {
      height += scrollPane.getHorizontalScrollBar().getPreferredSize().height;
    }
    return new Dimension(width, height);
  }

  private static Rectangle screenRectOfVisiblePart(Component owner, Rectangle fallback) {
    if (!owner.isShowing()) {
      return fallback;
    }
    Rectangle visible = owner instanceof JComponent ? ((JComponent) owner).getVisibleRect() : new Rectangle(owner.getSize());
    Point origin = owner.getLocationOnScreen();
    return new Rectangle(origin.x + visible.x, origin.y + visible.y, visible.width, visible.height);
  }

  /** Esc puts a box which is being dragged back first, or stops a connection which is being drawn, then puts the tool
   * away, then takes the selection away, and leaves the editor if there is none. */
  private void escape() {
    if (editing.cancelDrag()) {
      showDiagram();
    }
    else if (editing.isToolArmed()) {
      editing.disarmTool();
      showDiagram();
    }
    else if (editing.hasSelection()) {
      editing.deselect();
      showDiagram();
    }
    else {
      dispose();
    }
  }

  private void showDiagram() {
    canvas.setScene(editing.scene());
    canvas.setSelection(editing.selectionMarks());
    palette.showToolsArmed(editing.isRelationToolArmed() ? editing.armedRelationType() : null,
      editing.isAnchorToolArmed(), editing.isAssocClassToolArmed());
    canvas.refreshCursor();
  }

  /** Runs the operation on the key and shows the diagram as it is afterwards. While a name is typed, the keys belong
   * to the text field. */
  private void bindKey(int keyCode, int modifiers, Runnable operation) {
    getRootPane().registerKeyboardAction(e -> {
      if (!textEditor.isEditing()) {
        operation.run();
        showDiagram();
      }
    }, KeyStroke.getKeyStroke(keyCode, modifiers), JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  private void showContextMenu(Point2D at) {
    Rectangle where = canvas.toCanvas(new Rectangle2D.Double(at.getX(), at.getY(), 0, 0));
    RelationEnd_V001 end = editing.endHandleAt(at, canvas.toDiagramLength(PointerControl.HIT_PIXELS));
    JPopupMenu menu;
    SceneTextArea text = editing.movableTextAt(at);
    if (end != null) {
      menu = new LineEndContextMenu(editing, end, texts, this::showDiagram);
    }
    else if (text != null) {
      menu = new TextContextMenu(editing, text, texts, this::showDiagram);
    }
    else if (editing.selectedClass() != null) {
      menu = new ClassContextMenu(editing, editing.selectedClass(), textEditor, texts, this::pickColor,
        this::showDiagram);
    }
    else {
      menu = new NoteContextMenu(editing, editing.selectedNote(), texts, this::showDiagram);
    }
    menu.show(canvas, where.x, where.y);
  }

  private Color pickColor(Color start) {
    return JColorChooser.showDialog(this, texts.get("menu.class.color.title"), start);
  }

  private void relationToolToggled(RelationType_V001 type) {
    editing.toggleRelationTool(type);
    showDiagram();
  }

  private void relationTypeChosen(RelationType_V001 type) {
    editing.armRelationTool(type);
    showDiagram();
  }

  private void anchorToolToggled() {
    editing.toggleAnchorTool();
    showDiagram();
  }

  private void assocClassToolToggled() {
    editing.toggleAssocClassTool();
    showDiagram();
  }

  private void classDropped(ClassKind_V001 kind, Point screenPoint) {
    boxDropped(screenPoint, (x, y) -> textEditor.startNewClass(x, y, kind));
  }

  /** @param startBox puts the new box at the point of the diagram, in whole units, and starts to type its text */
  private void boxDropped(Point screenPoint, BiConsumer<Integer, Integer> startBox) {
    JViewport viewport = scrollPane.getViewport();
    Point inViewport = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(inViewport, viewport);
    if (!viewport.contains(inViewport)) {
      return;
    }
    Point inCanvas = new Point(screenPoint);
    SwingUtilities.convertPointFromScreen(inCanvas, canvas);
    Point2D inDiagram = canvas.toDiagram(inCanvas);
    startBox.accept((int) Math.round(inDiagram.getX()), (int) Math.round(inDiagram.getY()));
  }
}
