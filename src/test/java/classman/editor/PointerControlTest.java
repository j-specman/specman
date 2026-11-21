package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    editing = new DiagramEditing(SOURCE, host, new EditorTexts(host.locale()));
    control = new PointerControl(editing, () -> { }, nameEdits::incrementAndGet);
  }

  /** A double click on the name of a class starts the name editor. */
  @Test
  void doubleClickOnName() throws ClassDiagramParseException {
    open();
    editing.selectAt(new Point2D.Double(15, 25));
    Rectangle2D name = editing.selectedNameArea();

    control.doubleClicked(new Point2D.Double(name.getCenterX(), name.getCenterY()));

    assertEquals(1, nameEdits.get());
  }

  /** On the stereotype, on the tagged values and on the attributes, a double click selects the class only. */
  @Test
  void doubleClickElsewhereInClass() throws ClassDiagramParseException {
    open();
    editing.selectAt(new Point2D.Double(15, 25));
    Rectangle2D box = editing.selectedArea();
    Rectangle2D name = editing.selectedNameArea();
    assertTrue(name.getMinY() > box.getMinY() + 4, "the stereotype is above the name");
    assertTrue(name.getMaxY() < box.getMaxY() - 4, "the tagged values and attributes are below the name");

    control.doubleClicked(new Point2D.Double(name.getCenterX(), box.getMinY() + 2));
    control.doubleClicked(new Point2D.Double(name.getCenterX(), name.getMaxY() + 2));
    control.doubleClicked(new Point2D.Double(name.getCenterX(), box.getMaxY() - 3));

    assertEquals(0, nameEdits.get());
    assertTrue(editing.hasSelection());
  }

  /** A note has no name to edit. */
  @Test
  void doubleClickOnNote() throws ClassDiagramParseException {
    open();

    control.doubleClicked(new Point2D.Double(405, 25));

    assertEquals(0, nameEdits.get());
    assertTrue(editing.hasSelection());
  }
}
