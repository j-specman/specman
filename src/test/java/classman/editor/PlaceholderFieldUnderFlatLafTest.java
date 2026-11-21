package classman.editor;

import classman.render.SceneTextArea;
import classman.render.TextKind;
import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.Test;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Specman runs with FlatLaf, which gives an empty text field a minimum width of its own. */
class PlaceholderFieldUnderFlatLafTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 -- 22222222\n}";

  /** The field starts as wide as the place, also where the look and feel would make an empty field wider. */
  @Test
  void fieldAsWideAsPlace() throws Exception {
    LookAndFeel before = UIManager.getLookAndFeel();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          UIManager.setLookAndFeel(new FlatLightLaf());
          RecordingEditorHost host = new RecordingEditorHost();
          DiagramEditing editing = new DiagramEditing(BARE, host, new EditorTexts(host.locale()));
          for (TextKind kind : new TextKind[] {TextKind.RELATION_LABEL, TextKind.FROM_MULTIPLICITY,
              TextKind.TO_MULTIPLICITY}) {
            DiagramCanvas canvas = new DiagramCanvas(editing.scene());
            SceneTextArea place = editing.scene().placeholderAreas.stream()
              .filter(text -> text.kind == kind).findFirst().orElseThrow();
            new TextEditor(canvas, editing, () -> { }).start(place);
            JTextComponent field = (JTextComponent) canvas.getComponent(0);
            Rectangle dashed = canvas.toCanvas(place.area);
            dashed.grow(canvas.placeholderMargin(), canvas.placeholderMargin());
            assertEquals(dashed.width, field.getWidth(), kind + " is as wide as its place");
            assertEquals(dashed.x, field.getX(), kind + " starts where its place starts");
          }
        }
        catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });
    }
    finally {
      UIManager.setLookAndFeel(before);
    }
  }
}
