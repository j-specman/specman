package classman.editor;

import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditorTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String RELATED = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 1 -> * 22222222 \"owns\"\n}";
  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n}";
  private static final int TEXT_OFFSET_X = 5;
  private static final int TEXT_OFFSET_Y = 3;
  private static final String LONG_TEXT = "A text which is much longer than the field was at the start of the editing";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;
  private DiagramCanvas canvas;
  private JTextComponent field;

  /** Opens the editor for the text of the kind on the event dispatch thread, where the deferred resizing runs too. */
  private void open(TextKind kind) throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(RELATED, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      canvas = new DiagramCanvas(editing.scene());
      new TextEditor(canvas, editing, () -> { }).start(textOfKind(kind));
      field = (JTextComponent) canvas.getComponent(0);
    });
  }

  private void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(action);
  }

  private SceneTextArea textOfKind(TextKind kind) {
    return editing.scene().textAreas.stream().filter(text -> text.kind == kind).findFirst().orElseThrow();
  }

  /** The text in the field stands where the text was in the diagram, for a relation label and for a class name. */
  @Test
  void fieldOverText() throws Exception {
    for (TextKind kind : new TextKind[] {TextKind.RELATION_LABEL, TextKind.FROM_MULTIPLICITY, TextKind.CLASS_NAME}) {
      open(kind);
      Rectangle text = canvas.toCanvas(textOfKind(kind).area);

      if (!kind.centered) {
        assertEquals(text.x - TEXT_OFFSET_X, field.getX(), kind.name());
      }
      assertEquals(text.y - TEXT_OFFSET_Y, field.getY(), kind.name());
      assertTrue(field.getHeight() < text.height + 12,
        kind + " is not much higher than its text: " + field.getHeight() + " for " + text.height);
    }
  }

  /** Typing a longer text widens the field, but a single line doesn't make it higher. */
  @Test
  void widenedFieldStaysOneLine() throws Exception {
    for (TextKind kind : new TextKind[] {TextKind.RELATION_LABEL, TextKind.CLASS_NAME}) {
      open(kind);
      int height = field.getHeight();
      int width = field.getWidth();

      onEdt(() -> field.setText(LONG_TEXT));
      onEdt(() -> { });

      assertTrue(field.getWidth() > width, kind + " is wider");
      assertEquals(height, field.getHeight(), kind + " stays as high");
    }
  }

  /** The field for a text which doesn't exist yet starts as wide as its place, and covers it, and then grows. It is as
   * high as the field for a text of the same kind which exists. */
  @Test
  void fieldCoversPlaceholder() throws Exception {
    for (TextKind kind : new TextKind[] {TextKind.RELATION_LABEL, TextKind.FROM_MULTIPLICITY,
        TextKind.TO_MULTIPLICITY}) {
      open(kind);
      int heightOfExistingText = field.getHeight();
      SceneTextArea[] place = new SceneTextArea[1];
      onEdt(() -> {
        try {
          editing = new DiagramEditing(BARE, host);
        }
        catch (Exception e) {
          throw new IllegalStateException(e);
        }
        canvas = new DiagramCanvas(editing.scene());
        place[0] = editing.scene().placeholderAreas.stream().filter(text -> text.kind == kind).findFirst().orElseThrow();
        new TextEditor(canvas, editing, () -> { }).start(place[0]);
        field = (JTextComponent) canvas.getComponent(0);
      });
      Rectangle dashed = canvas.toCanvas(place[0].area);
      dashed.grow(canvas.placeholderMargin(), canvas.placeholderMargin());

      assertEquals(dashed.width, field.getWidth(), kind + " is as wide as its place");
      assertEquals(dashed.x, field.getX(), kind + " starts where its place starts");
      assertEquals(heightOfExistingText, field.getHeight(), kind + " is as high as a field for an existing text");

      onEdt(() -> field.setText(LONG_TEXT));
      onEdt(() -> { });

      assertTrue(field.getWidth() > dashed.width, kind + " grows with the text");
    }
  }
}
