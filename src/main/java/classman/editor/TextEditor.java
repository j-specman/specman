package classman.editor;

import classman.render.DiagramFonts;
import classman.render.DiagramStyle;
import classman.render.SceneTextArea;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/** Lets the user type a text of the diagram, like the name of a class, into a text field which lies over the text in
 * the diagram, in the font of the text, aligned like it. White if changes are not tracked, otherwise in the pale color
 * of the change set. Return or leaving the field gives the text its new content, Esc leaves it as it was.
 * Ctrl+Return or Shift+Return make a line break, which stays in the text. The diagram is laid out again after that,
 * not while typing. */
class TextEditor {
  private static final int MIN_FIELD_WIDTH = 120;
  private static final int FIELD_MARGIN = 3;
  private static final int TEXT_MARGIN = 4;
  private static final int BORDER_WIDTH = 1;
  private static final Color BORDER_COLOR = new Color(0x2D7FF9);

  private final DiagramCanvas canvas;
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private JTextComponent field;
  private SceneTextArea edited;
  private Rectangle textArea;
  private Rectangle boxArea;

  /** @param showDiagram shows the diagram as the editing has it now */
  TextEditor(DiagramCanvas canvas, DiagramEditing editing, Runnable showDiagram) {
    this.canvas = canvas;
    this.editing = editing;
    this.showDiagram = showDiagram;
  }

  boolean isEditing() { return field != null; }

  /** Starts to edit the text. Nothing happens while another text is being edited. */
  void start(SceneTextArea text) {
    if (field != null) {
      return;
    }
    edited = text;
    textArea = canvas.toCanvas(text.area);
    boxArea = canvas.toCanvas(editing.boxOf(text));
    field = newField(text, editing.textOf(text), editing.trackedFieldColor());
    canvas.add(field);
    resizeToText();
    canvas.repaint();
    field.requestFocusInWindow();
    field.selectAll();
  }

  private JTextComponent newField(SceneTextArea text, String content, Color trackedColor) {
    JTextComponent component = text.kind.wrapped ? wrappingArea(content) : centeredPane(content);
    float size = (float) (DiagramStyle.FONT_SIZE * canvas.zoom());
    component.setFont(text.kind.bold ? DiagramFonts.bold(size) : DiagramFonts.plain(size));
    component.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, BORDER_WIDTH),
      BorderFactory.createEmptyBorder(1, TEXT_MARGIN, 1, TEXT_MARGIN)));
    component.setBackground(trackedColor != null ? trackedColor : Color.WHITE);
    component.getDocument().addDocumentListener(new DocumentChangeAdapter(
      () -> SwingUtilities.invokeLater(this::resizeToText)));
    component.addFocusListener(new FocusLostAdapter(this::commit));
    component.registerKeyboardAction(e -> commit(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
      JComponent.WHEN_FOCUSED);
    component.registerKeyboardAction(e -> component.replaceSelection("\n"),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);
    component.registerKeyboardAction(e -> component.replaceSelection("\n"),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_FOCUSED);
    component.registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_FOCUSED);
    return component;
  }

  /** A text which is wrapped at the width of its box is typed into a text area which wraps at words. */
  private static JTextArea wrappingArea(String content) {
    JTextArea area = new JTextArea(content);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    return area;
  }

  /** A text which makes its box wider is typed into a text pane, because that can center the text like the diagram. */
  private static JTextPane centeredPane(String content) {
    JTextPane pane = new JTextPane();
    pane.setText(content);
    StyledDocument document = pane.getStyledDocument();
    SimpleAttributeSet alignment = new SimpleAttributeSet();
    StyleConstants.setAlignment(alignment, StyleConstants.ALIGN_CENTER);
    document.setParagraphAttributes(0, document.getLength(), alignment, false);
    return pane;
  }

  /** The field is as large as its text needs, at least wide enough to type. A text which is wrapped in its box has
   * the width of the box, and grows downwards. A text which makes its box wider is at least as wide as it was, a
   * centered one stays centered, any other starts where it started. The top is where the top of the text is. */
  private void resizeToText() {
    if (field == null) {
      return;
    }
    Rectangle bounds = edited.kind.wrapped ? wrappedBounds() : growingBounds();
    if (!bounds.equals(field.getBounds())) {
      field.setBounds(bounds);
    }
  }

  private Rectangle wrappedBounds() {
    int width = Math.max(boxArea.width, MIN_FIELD_WIDTH);
    int height = Math.max(textArea.height + 2 * FIELD_MARGIN, heightAtWidth(width));
    return new Rectangle(boxArea.x, textArea.y - FIELD_MARGIN, width, height);
  }

  /** The height the text needs when it is wrapped at the given width of the field. The field must be higher than its
   * borders while it is asked, otherwise it takes the width of its borders as its width. */
  private int heightAtWidth(int width) {
    field.setSize(width, textArea.height + 2 * FIELD_MARGIN);
    return field.getPreferredSize().height;
  }

  private Rectangle growingBounds() {
    Dimension wanted = field.getPreferredSize();
    int width = Math.max(Math.max(textArea.width, MIN_FIELD_WIDTH), wanted.width);
    int height = Math.max(textArea.height + 2 * FIELD_MARGIN, wanted.height);
    int x = edited.kind.centered
      ? textArea.x + (textArea.width - width) / 2
      : textArea.x - TEXT_MARGIN - BORDER_WIDTH;
    return new Rectangle(x, textArea.y - FIELD_MARGIN, width, height);
  }

  /** An empty text is no text: the element keeps its own. Tabs which were typed or pasted become spaces. */
  private void commit() {
    if (field == null) {
      return;
    }
    String content = field.getText().replace('\t', ' ').strip();
    SceneTextArea text = edited;
    close();
    if (!content.isEmpty()) {
      editing.setText(text, content);
    }
    showDiagram.run();
  }

  private void cancel() {
    close();
  }

  private void close() {
    JTextComponent closing = field;
    field = null;
    edited = null;
    canvas.remove(closing);
    canvas.repaint();
    canvas.requestFocusInWindow();
  }
}
