package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointerControlTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String SOURCE = "classdiagram {\n  class Book (04de91aa) at(10, 20) <<Entity>> {TABLE=books} {\n"
    + "    -title : String\n  }\n  note (22222222) at(400, 20) \"Remember\"\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final AtomicInteger nameEdits = new AtomicInteger();
  private DiagramEditing editing;
  private PointerControl control;

  private void open() throws ClassDiagramParseException {
    open(SOURCE);
  }

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host, new EditorTexts(host.locale()));
    control = new PointerControl(editing, () -> { }, text -> nameEdits.incrementAndGet(), () -> 4);
  }

  /** A double click on the label of a relation starts the editor for it, although no box is selected. */
  @Test
  void doubleClickOnRelationLabel() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n  class B (22222222) at(300, 20)\n"
      + "  association (33333333) 11111111 1 -> * 22222222 \"owns\"\n}");
    Rectangle2D label = editing.scene().textAreas.stream()
      .filter(area -> area.kind == TextKind.RELATION_LABEL).findFirst().orElseThrow().area;

    control.doubleClicked(new Point2D.Double(label.getMinX() - 20, label.getMinY() - 20));
    assertEquals(0, nameEdits.get());

    control.doubleClicked(new Point2D.Double(label.getCenterX(), label.getCenterY()));

    assertEquals(1, nameEdits.get());
    assertFalse(editing.hasSelection());
  }

  /** A double click on the name of a class starts the name editor. */
  @Test
  void doubleClickOnName() throws ClassDiagramParseException {
    open();
    editing.selectAt(new Point2D.Double(15, 25));
    Rectangle2D name = editing.scene().textAreas.get(0).area;

    control.doubleClicked(new Point2D.Double(name.getCenterX(), name.getCenterY()));

    assertEquals(1, nameEdits.get());
  }

  /** On the stereotype, on the tagged values and on the attributes, a double click selects the class only. */
  @Test
  void doubleClickElsewhereInClass() throws ClassDiagramParseException {
    open();
    editing.selectAt(new Point2D.Double(15, 25));
    Rectangle2D box = editing.selectedArea();
    Rectangle2D name = editing.scene().textAreas.get(0).area;
    assertTrue(name.getMinY() > box.getMinY() + 4, "the stereotype is above the name");
    assertTrue(name.getMaxY() < box.getMaxY() - 4, "the tagged values and attributes are below the name");

    control.doubleClicked(new Point2D.Double(name.getCenterX(), box.getMinY() + 2));
    control.doubleClicked(new Point2D.Double(name.getCenterX(), name.getMaxY() + 2));
    control.doubleClicked(new Point2D.Double(name.getCenterX(), box.getMaxY() - 3));

    assertEquals(0, nameEdits.get());
    assertTrue(editing.hasSelection());
  }

  /** A double click on the text of a note starts the editor for it, one on the margin of the note does not. */
  @Test
  void doubleClickOnNote() throws ClassDiagramParseException {
    open();
    editing.selectAt(new Point2D.Double(401, 21));
    Rectangle2D noteBox = editing.selectedArea();

    control.doubleClicked(new Point2D.Double(noteBox.getMinX() + 1, noteBox.getMinY() + 1));
    assertEquals(0, nameEdits.get());

    Rectangle2D text = editing.scene().textAreas.stream()
      .filter(area -> area.kind == TextKind.NOTE_TEXT).findFirst().orElseThrow().area;
    control.doubleClicked(new Point2D.Double(text.getCenterX(), text.getCenterY()));

    assertEquals(1, nameEdits.get());
    assertTrue(editing.hasSelection());
  }
}
