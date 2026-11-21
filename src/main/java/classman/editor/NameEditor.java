package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.render.DiagramFonts;
import classman.render.DiagramStyle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;

/** Lets the user type the name of a class into a text field over the name in the diagram. White
 * if changes are not tracked, otherwise in the pale color of the change set. Return or leaving the field gives the class
 * the name, Esc leaves it as it was. Ctrl+Return or Shift+Return make a line break, which stays in the name. The diagram
 * is laid out again after that, not while typing. */
class NameEditor {
  private static final int MIN_FIELD_WIDTH = 120;
  private static final int FIELD_MARGIN = 3;
  private static final int TEXT_MARGIN = 4;
  private static final Color BORDER_COLOR = new Color(0x2D7FF9);

  private final DiagramCanvas canvas;
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private JTextPane field;
  private ClassModel_V001 editedClass;
  private Rectangle nameArea;

  /** @param showDiagram shows the diagram as the editing has it now */
  NameEditor(DiagramCanvas canvas, DiagramEditing editing, Runnable showDiagram) {
    this.canvas = canvas;
    this.editing = editing;
    this.showDiagram = showDiagram;
  }

  boolean isEditing() { return field != null; }

  /** Starts to edit the name of the selected class, nothing happens if no class is selected. */
  void start() {
    Rectangle2D selectedNameArea = editing.selectedNameArea();
    if (field != null || editing.selectedClass() == null || selectedNameArea == null) {
      return;
    }
    editedClass = editing.selectedClass();
    nameArea = canvas.toCanvas(selectedNameArea);
    field = newField(editedClass.name, editing.trackedFieldColor());
    canvas.add(field);
    resizeToText();
    canvas.repaint();
    field.requestFocusInWindow();
    field.selectAll();
  }

  private JTextPane newField(String name, Color trackedColor) {
    JTextPane pane = new JTextPane();
    pane.setFont(DiagramFonts.bold((float) (DiagramStyle.FONT_SIZE * canvas.zoom())));
    pane.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR),
      BorderFactory.createEmptyBorder(1, TEXT_MARGIN, 1, TEXT_MARGIN)));
    pane.setText(name);
    pane.setBackground(trackedColor != null ? trackedColor : Color.WHITE);
    centerParagraphs(pane);
    pane.getDocument().addDocumentListener(new DocumentChangeAdapter(
      () -> SwingUtilities.invokeLater(this::resizeToText)));
    pane.addFocusListener(new FocusLostAdapter(this::commit));
    pane.registerKeyboardAction(e -> commit(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
    pane.registerKeyboardAction(e -> pane.replaceSelection("\n"),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);
    pane.registerKeyboardAction(e -> pane.replaceSelection("\n"),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_FOCUSED);
    pane.registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_FOCUSED);
    return pane;
  }

  /** The class names are centered in the diagram, so they are centered while typed. */
  private static void centerParagraphs(JTextPane pane) {
    StyledDocument document = pane.getStyledDocument();
    SimpleAttributeSet centered = new SimpleAttributeSet();
    StyleConstants.setAlignment(centered, StyleConstants.ALIGN_CENTER);
    document.setParagraphAttributes(0, document.getLength(), centered, false);
  }

  /** The field is as large as its text needs, at least as wide as the name was and wide enough to type, and stays
   * centered over the name with the top where the name's top is. */
  private void resizeToText() {
    if (field == null) {
      return;
    }
    Dimension wanted = field.getPreferredSize();
    int width = Math.max(Math.max(nameArea.width, MIN_FIELD_WIDTH), wanted.width);
    int height = Math.max(nameArea.height + 2 * FIELD_MARGIN, wanted.height);
    Rectangle bounds = new Rectangle(nameArea.x + (nameArea.width - width) / 2, nameArea.y - FIELD_MARGIN, width, height);
    if (!bounds.equals(field.getBounds())) {
      field.setBounds(bounds);
    }
  }

  /** An empty name is no name: the class keeps its own. Tabs which were typed or pasted become spaces. */
  private void commit() {
    if (field == null) {
      return;
    }
    String name = field.getText().replace('\t', ' ').strip();
    ClassModel_V001 box = editedClass;
    close();
    if (!name.isEmpty()) {
      editing.rename(box, name);
    }
    showDiagram.run();
  }

  private void cancel() {
    close();
  }

  private void close() {
    JTextPane closing = field;
    field = null;
    editedClass = null;
    canvas.remove(closing);
    canvas.repaint();
    canvas.requestFocusInWindow();
  }
}
