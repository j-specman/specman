package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.render.DiagramFonts;
import classman.render.DiagramStyle;
import classman.render.SceneTextArea;
import classman.render.TextKind;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/** Lets the user type a text of the diagram, like the name of a class, into a text field which lies over the text in
 * the diagram, in the font of the text, aligned like it. White if changes are not tracked, otherwise in the pale color
 * of the change set. Return or leaving the field gives the text its new content, Esc leaves it as it was.
 * Ctrl+Return or Shift+Return make a line break, which stays in the text. The diagram is laid out again after that,
 * not while typing. */
class TextEditor {
  private static final int FIELD_MARGIN = 3;
  private static final int TEXT_MARGIN = 4;
  private static final int BORDER_WIDTH = 1;
  private static final int CARET_WIDTH = 2;
  private static final Color BORDER_COLOR = new Color(0x2D7FF9);
  private static final Color REJECTED_BORDER_COLOR = new Color(0xD32F2F);

  private final DiagramCanvas canvas;
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private JTextComponent field;
  private SceneTextArea edited;
  private Rectangle textArea;
  private Rectangle boxArea;
  private boolean rejected;

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
    JTextComponent component = text.kind.wrapped ? wrappingArea(content) : textPane(content, text.kind.centered);
    float size = (float) (DiagramStyle.FONT_SIZE * canvas.zoom());
    component.setFont(text.kind.bold ? DiagramFonts.bold(size) : DiagramFonts.plain(size));
    component.setBorder(border(BORDER_COLOR));
    component.setBackground(trackedColor != null ? trackedColor : Color.WHITE);
    component.getDocument().addDocumentListener(new DocumentChangeAdapter(() -> {
      showAsAccepted();
      SwingUtilities.invokeLater(this::resizeToText);
    }));
    component.addFocusListener(new FocusLostAdapter(() -> {
      if (field == component) {
        commit(false);
      }
    }));
    component.registerKeyboardAction(e -> commit(true), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
      JComponent.WHEN_FOCUSED);
    if (text.kind == TextKind.ATTRIBUTE_LINE) {
      ((AbstractDocument) component.getDocument()).setDocumentFilter(new SingleLineFilter());
    }
    else {
      component.registerKeyboardAction(e -> component.replaceSelection("\n"),
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);
      component.registerKeyboardAction(e -> component.replaceSelection("\n"),
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_FOCUSED);
    }
    component.registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_FOCUSED);
    return component;
  }

  /** The frame of the field, and inside it the text which the diagram draws around the edited one, if any. */
  private Border border(Color color) {
    Border frame = BorderFactory.createCompoundBorder(new LineBorder(color, BORDER_WIDTH),
      BorderFactory.createEmptyBorder(1, TEXT_MARGIN, 1, TEXT_MARGIN));
    TextKind kind = edited.kind;
    boolean hasBrackets = !kind.prefix.isEmpty() || !kind.suffix.isEmpty();
    return hasBrackets ? BorderFactory.createCompoundBorder(frame, new BracketBorder(kind.prefix, kind.suffix)) : frame;
  }

  private void showAsAccepted() {
    if (rejected && field != null) {
      rejected = false;
      field.setBorder(border(BORDER_COLOR));
    }
  }

  /** A text which is wrapped at the width of its box is typed into a text area which wraps at words. */
  private static JTextArea wrappingArea(String content) {
    JTextArea area = new JTextArea(content);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    return area;
  }

  /** A text which makes its box wider is typed into a text pane, because that can center the text like the diagram. */
  private static JTextPane textPane(String content, boolean centered) {
    JTextPane pane = new JTextPane();
    pane.setText(content);
    StyledDocument document = pane.getStyledDocument();
    SimpleAttributeSet alignment = new SimpleAttributeSet();
    StyleConstants.setAlignment(alignment, centered ? StyleConstants.ALIGN_CENTER : StyleConstants.ALIGN_LEFT);
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

  /** The width a field has at least: it grows and shrinks with the zoom, like the font in it. */
  private int minFieldWidth() {
    return (int) Math.round(edited.kind.minFieldWidth * canvas.zoom());
  }

  private Rectangle wrappedBounds() {
    int width = Math.max(boxArea.width, minFieldWidth());
    int height = Math.max(textArea.height + 2 * FIELD_MARGIN, heightAtWidth(width));
    return new Rectangle(boxArea.x, textArea.y - FIELD_MARGIN, width, height);
  }

  /** The height the text needs when it is wrapped at the given width of the field. The field must be higher than its
   * borders while it is asked, otherwise it takes the width of its borders as its width. */
  private int heightAtWidth(int width) {
    field.setSize(width, textArea.height + 2 * FIELD_MARGIN);
    return field.getPreferredSize().height;
  }

  /** The width a text field starts with: at least the width of a text which exists, and at least what its text needs.
   * The field for a text which doesn't exist yet starts as wide as its place, which the user sees as a dashed
   * rectangle around the area of the text, and covers it exactly sideways. It is as high as the field for a text which
   * exists, the dashed rectangle is higher than that. */
  private Rectangle growingBounds() {
    int margin = edited.placeholder ? canvas.placeholderMargin() : 0;
    int minimumWidth = edited.placeholder
      ? textArea.width + 2 * margin
      : Math.max(textArea.width, minFieldWidth());
    int width = Math.max(minimumWidth, widthOfText());
    int height = Math.max(textArea.height + 2 * FIELD_MARGIN, heightAtWidth(width));
    int x = edited.kind.centered
      ? textArea.x + (textArea.width - width) / 2
      : textArea.x - (edited.placeholder ? margin : TEXT_MARGIN + BORDER_WIDTH);
    return new Rectangle(x, textArea.y - FIELD_MARGIN, width, height);
  }

  /** The width which the lines of the text and the borders of the field need. The preferred width of the field is no
   * help: some looks and feels give an empty field a minimum width of their own, which would make a field for a short
   * text wider than the text it replaces. */
  private int widthOfText() {
    FontMetrics metrics = field.getFontMetrics(field.getFont());
    int widest = 0;
    for (String line : field.getText().split("\n", -1)) {
      widest = Math.max(widest, metrics.stringWidth(line));
    }
    Insets borders = field.getInsets();
    return widest + borders.left + borders.right + CARET_WIDTH;
  }

  /** An empty text is no text: a mandatory text keeps its old content, an optional one is removed. Tabs which were
   * typed or pasted become spaces. A content which the text can't have keeps the field open with a red border if the
   * user pressed Return, and is dropped if the field lost the focus. Return in the last line of the attributes of a
   * class goes on with a blank line below it for the next attribute, an empty line, Esc or leaving the field ends that.
   * @param returnPressed true if the user pressed Return */
  private void commit(boolean returnPressed) {
    if (field == null) {
      return;
    }
    String content = field.getText().replace('\t', ' ').strip();
    SceneTextArea text = edited;
    boolean unchanged = content.isEmpty() && !text.kind.optional;
    boolean accepted = unchanged || editing.accepts(text, content);
    if (!accepted && returnPressed) {
      rejected = true;
      field.setBorder(border(REJECTED_BORDER_COLOR));
      return;
    }
    boolean goOn = returnPressed && accepted && !content.isEmpty() && editing.isLastAttributeLine(text);
    close();
    if (accepted && !unchanged) {
      editing.setText(text, content);
    }
    editing.discardNewPart();
    if (goOn) {
      startNewAttribute((ClassModel_V001) text.element);
      return;
    }
    showDiagram.run();
  }

  /** Puts a class without a name at the point of the diagram and starts to type the name. A text which is being typed
   * is finished first. */
  void startNewClass(int x, int y) {
    commit(false);
    SceneTextArea name = editing.newClass(x, y);
    showDiagram.run();
    start(name);
  }

  /** Puts a blank line at the end of the class and starts to type a new attribute into it. */
  void startNewAttribute(ClassModel_V001 box) {
    SceneTextArea line = editing.newAttributeAtEnd(box);
    showDiagram.run();
    start(line);
  }

  /** Puts a blank row for the stereotype into the class and starts to type it. A stereotype which a change set has
   * removed is typed into its struck row. */
  void startNewStereotype(ClassModel_V001 box) {
    SceneTextArea row = editing.newStereotype(box);
    showDiagram.run();
    start(row);
  }

  private void cancel() {
    close();
    editing.discardNewPart();
    showDiagram.run();
  }

  private void close() {
    JTextComponent closing = field;
    field = null;
    edited = null;
    rejected = false;
    canvas.remove(closing);
    canvas.repaint();
    canvas.requestFocusInWindow();
  }
}
