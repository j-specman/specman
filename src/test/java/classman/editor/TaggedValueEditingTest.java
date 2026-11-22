package classman.editor;

import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneRectangle;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaggedValueEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n}";
  private static final String TWO = "classdiagram {\n  class A (11111111) at(10, 60) {TABLE=books, author=Jan}\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(area.getCenterX(), area.getMaxY() - 2));
  }

  private List<SceneTextArea> rows() {
    return editing.scene().textAreas.stream().filter(area -> area.kind == TextKind.TAGGED_VALUE).toList();
  }

  private ClassModel_V001 classOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).classes().get(0);
  }

  /** Every tagged value has a row, in the order of the source, and its text is KEY=value. */
  @Test
  void rowsOfTaggedValues() throws ClassDiagramParseException {
    open(TWO);

    assertEquals(List.of("TABLE=books", "author=Jan"), rows().stream().map(editing::textOf).toList());
    assertTrue(rows().get(0).area.getMaxY() <= rows().get(1).area.getMinY());
  }

  /** The row for a new tagged value comes after the last one, and is not in the diagram until it has a text. */
  @Test
  void newRow() throws ClassDiagramParseException {
    open(TWO);

    SceneTextArea row = editing.newTaggedValue(editing.selectedClass());

    assertEquals(3, rows().size());
    assertEquals(row, rows().get(2));
    assertEquals("", editing.textOf(row));
    assertEquals(2, classOf(editing.source()).taggedValues.size());
    assertFalse(editing.canUndo());
  }

  /** The text creates the tagged value at the end, marked as added while changes are tracked. One operation. */
  @Test
  void createdTaggedValue() throws ClassDiagramParseException {
    open(TWO);
    editing.setText(editing.newTaggedValue(editing.selectedClass()), "v = 1");
    assertEquals("v", classOf(editing.source()).taggedValues.get(2).key);
    assertEquals("1", classOf(editing.source()).taggedValues.get(2).value);
    assertNull(classOf(editing.source()).taggedValues.get(2).change);
    assertEquals(1, host.reportedSources.size());

    host.trackedChangeSet = "yellow";
    open(TWO);
    editing.setText(editing.newTaggedValue(editing.selectedClass()), "v=1");
    assertEquals(ChangeType_V001.ADDED, classOf(editing.source()).taggedValues.get(2).change.type);
    assertEquals("yellow", classOf(editing.source()).taggedValues.get(2).change.changeSet);
  }

  /** The row of a tagged value which the change set has added has the strong color of the change set behind it. */
  @Test
  void addedRowIsColored() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    host.changeColors.strongColors.put("yellow", "#ffff00");
    open(TWO);

    editing.setText(editing.newTaggedValue(editing.selectedClass()), "v=1");

    long colored = editing.scene().elements.stream()
      .filter(element -> element instanceof SceneRectangle rectangle && "#ffff00".equals(rectangle.fill)).count();
    assertEquals(1, colored);
  }

  /** The empty text, or dropping the row, leaves the class as it was and is no operation. */
  @Test
  void droppedRow() throws ClassDiagramParseException {
    open(TWO);
    String before = editing.source();

    editing.setText(editing.newTaggedValue(editing.selectedClass()), "");
    assertEquals(2, rows().size());
    editing.newTaggedValue(editing.selectedClass());
    editing.discardNewPart();

    assertEquals(2, rows().size());
    assertEquals(before, editing.source());
    assertFalse(editing.canUndo());
  }

  /** What isn't KEY=value is not accepted, the empty text is. */
  @Test
  void invalidTexts() throws ClassDiagramParseException {
    open(TWO);
    SceneTextArea row = rows().get(0);

    assertTrue(editing.accepts(row, "books"));
    assertFalse(editing.accepts(row, "=books"));
    assertFalse(editing.accepts(row, "my key=books"));
    assertFalse(editing.accepts(row, "key="));
    assertTrue(editing.accepts(row, "key=a b"));
    assertTrue(editing.accepts(row, ""));
    assertThrows(IllegalArgumentException.class, () -> editing.setText(row, "books="));
  }

  /** A tagged value with only a key is created, shown and typed as the key alone. */
  @Test
  void createdKeyOnly() throws ClassDiagramParseException {
    open(TWO);

    editing.setText(editing.newTaggedValue(editing.selectedClass()), "persistent");

    assertEquals("persistent", classOf(editing.source()).taggedValues.get(2).key);
    assertNull(classOf(editing.source()).taggedValues.get(2).value);
    assertEquals("persistent", editing.textOf(rows().get(2)));
    assertFalse(editing.accepts(rows().get(2), "persistent="));
  }

  /** A tagged value is changed by typing it again. While changes are tracked, the old value is the backup. */
  @Test
  void changedTaggedValue() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(TWO);

    editing.setText(rows().get(1), "author=Anna");

    ClassModel_V001 box = classOf(editing.source());
    assertEquals("Jan", box.taggedValues.get(1).valueChange.backup);
    assertNull(box.taggedValues.get(1).keyChange);
    assertNull(box.taggedValues.get(0).valueChange);
  }

  /** The empty text deletes a tagged value. Without tracking it is gone, with tracking it keeps its text and is
   * marked as removed, and has no row to change any more. */
  @Test
  void deletedTaggedValue() throws ClassDiagramParseException {
    open(TWO);
    editing.setText(rows().get(0), "");
    assertEquals(List.of("author"), classOf(editing.source()).taggedValues.stream().map(tag -> tag.key).toList());

    host.trackedChangeSet = "yellow";
    open(TWO);
    editing.setText(rows().get(0), "");

    ClassModel_V001 box = classOf(editing.source());
    assertEquals(ChangeType_V001.REMOVED, box.taggedValues.get(0).change.type);
    assertEquals("TABLE", box.taggedValues.get(0).key);
    assertEquals(1, rows().size());
    assertEquals("author=Jan", editing.textOf(rows().get(0)));
  }

  /** A tagged value which the change set has added is gone without a trace when it is deleted. */
  @Test
  void addedTaggedValueIsDeletedForGood() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(BARE);
    editing.setText(editing.newTaggedValue(editing.selectedClass()), "v=1");

    editing.setText(rows().get(0), "");

    assertTrue(classOf(editing.source()).taggedValues.isEmpty());
  }

  /** Whatever else is done while the row is there, the diagram which is written has no blank tagged value. */
  @Test
  void rowIsNeverWritten() throws ClassDiagramParseException {
    open(TWO);
    editing.newTaggedValue(editing.selectedClass());

    editing.setText(editing.newClass(300, 60), "B");

    assertEquals(2, classOf(editing.source()).taggedValues.size());
  }

  /** A class which a change set has removed can't get a tagged value. */
  @Test
  void removedClassGetsNone() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 60)\n}");

    assertThrows(IllegalArgumentException.class, () -> editing.newTaggedValue(editing.selectedClass()));
  }

  /** In the field of a tagged value there is no line break, and Return commits the row. */
  @Test
  void fieldOfTaggedValue() throws Exception {
    open(TWO);
    DiagramCanvas[] canvas = new DiagramCanvas[1];
    SwingUtilities.invokeAndWait(() -> {
      canvas[0] = new DiagramCanvas(editing.scene());
      new TextEditor(canvas[0], editing, () -> canvas[0].setScene(editing.scene())).start(rows().get(1));
    });
    JTextComponent field = (JTextComponent) canvas[0].getComponent(0);

    SwingUtilities.invokeAndWait(() -> {
      field.setText("author=Anna\nBerta");
      field.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        .actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, ""));
    });
    SwingUtilities.invokeAndWait(() -> { });

    assertEquals("Anna Berta", classOf(editing.source()).taggedValues.get(1).value);
    assertEquals(0, canvas[0].getComponentCount());
  }
}
