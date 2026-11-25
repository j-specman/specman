package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Typing the lines of the attributes of a class into the text field, one after the other. */
class AttributeTextEditorTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BOOK = "classdiagram {\n  class Book (04de91aa) at(10, 20) {\n"
    + "    -title : String [Pflicht]\n    -pages\n  }\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private DiagramCanvas canvas;
  private TextEditor editor;

  /** Opens the editor for the attribute line of the class on the event dispatch thread. */
  private void open(int lineIndex) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(BOOK, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      canvas = new DiagramCanvas(editing.scene());
      editor = new TextEditor(canvas, editing, () -> canvas.setScene(editing.scene()));
      editor.start(lines().get(lineIndex));
    });
  }

  private List<SceneTextArea> lines() {
    return editing.scene().textAreas.stream().filter(text -> text.kind == TextKind.ATTRIBUTE_LINE).toList();
  }

  private JTextComponent field() {
    return (JTextComponent) canvas.getComponent(0);
  }

  private void typeAndPressReturn(String text) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      field().setText(text);
      field().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        .actionPerformed(new ActionEvent(field(), ActionEvent.ACTION_PERFORMED, ""));
    });
    SwingUtilities.invokeAndWait(() -> { });
  }

  private List<String> attributeNames() throws Exception {
    ClassModel_V001 book = new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
    return book.attributes.stream().map(attribute -> attribute.name).toList();
  }

  /** The field lies over the whole line, starting at the top of its text, and shows the line. */
  @Test
  void fieldOverLine() throws Exception {
    open(0);
    Rectangle line = canvas.toCanvas(lines().get(0).area);
    Rectangle box = canvas.toCanvas(editing.boxOf(lines().get(0)));

    assertEquals("-title : String [Pflicht]", field().getText());
    assertEquals(box.x, field().getX());
    assertEquals(line.y - 3, field().getY());
    assertTrue(field().getWidth() >= box.width);
  }

  /** An attribute takes one line: Ctrl+Return and Shift+Return make no line break, and pasted line breaks become
   * blanks. */
  @Test
  void noLineBreaksInAttribute() throws Exception {
    open(0);

    SwingUtilities.invokeAndWait(() -> {
      field().setText("-a\n: String\r\n[x]");
      for (int modifiers : new int[] {java.awt.event.InputEvent.CTRL_DOWN_MASK, java.awt.event.InputEvent.SHIFT_DOWN_MASK}) {
        java.awt.event.ActionListener action = field().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers));
        if (action != null) {
          action.actionPerformed(new ActionEvent(field(), ActionEvent.ACTION_PERFORMED, ""));
        }
      }
    });

    String text = field().getText();
    assertTrue(!text.contains("\n") && !text.contains("\r"), text);
    assertEquals("-a : String [x]", text.strip());
  }

  /** Return in a line which isn't the last commits it and ends. */
  @Test
  void returnInMiddleLine() throws Exception {
    open(0);

    typeAndPressReturn("-headline : String");

    assertEquals(List.of("headline", "pages"), attributeNames());
    assertEquals(0, canvas.getComponentCount());
  }

  /** Return in the last line commits it and opens a blank line below for the next attribute, and so on until that
   * stays empty. */
  @Test
  void returnInLastLineGoesOn() throws Exception {
    open(1);
    Rectangle lastLine = canvas.toCanvas(lines().get(1).area);

    typeAndPressReturn("-isbn : String");

    assertEquals(List.of("title", "isbn"), attributeNames());
    assertEquals(1, canvas.getComponentCount());
    assertEquals("", field().getText());
    assertTrue(field().getY() > lastLine.y, "the blank line is below the line which was typed");

    typeAndPressReturn("~pages");

    assertEquals(List.of("title", "isbn", "pages"), attributeNames());
    assertEquals("", field().getText());

    typeAndPressReturn("");

    assertEquals(List.of("title", "isbn", "pages"), attributeNames());
    assertEquals(0, canvas.getComponentCount());
    assertEquals(2, host.reportedSources.size());
    assertEquals(3, lines().size());
  }

  /** Esc ends the blank line without a new attribute, and the lines which were committed stay. */
  @Test
  void escapeEndsBlankLine() throws Exception {
    open(1);
    typeAndPressReturn("-isbn");
    double heightWithBlank = editing.scene().height;

    SwingUtilities.invokeAndWait(() -> field().getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0))
      .actionPerformed(new ActionEvent(field(), ActionEvent.ACTION_PERFORMED, "")));

    assertEquals(List.of("title", "isbn"), attributeNames());
    assertEquals(0, canvas.getComponentCount());
    assertEquals(2, lines().size());
    assertTrue(editing.scene().height < heightWithBlank);
  }

  /** A line without a name keeps the field open, nothing is changed. */
  @Test
  void invalidLineKeepsFieldOpen() throws Exception {
    open(0);

    typeAndPressReturn(": String");

    assertEquals(1, canvas.getComponentCount());
    assertEquals(List.of("title", "pages"), attributeNames());
    assertTrue(host.reportedSources.isEmpty());
  }
}
