package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeEditingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n}";
  private static final String WITH_STEREOTYPE = "classdiagram {\n  class A (11111111) at(10, 60) <<Entity>>\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host, new EditorTexts(host.locale()));
  }

  private Rectangle2D boxArea() {
    return editing.scene().elementAreas.get(0).area;
  }

  private void selectClass() {
    editing.selectAt(new Point2D.Double(boxArea().getCenterX(), boxArea().getMaxY() - 2));
  }

  private SceneTextArea stereotypeText() {
    return editing.scene().textAreas.stream().filter(area -> area.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();
  }

  private boolean hasStereotypeText() {
    return editing.scene().textAreas.stream().anyMatch(area -> area.kind == TextKind.STEREOTYPE);
  }

  private static Point2D centerOf(SceneTextArea area) {
    return new Point2D.Double(area.area.getCenterX(), area.area.getCenterY());
  }

  private ClassModel_V001 classOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).classes().get(0);
  }

  /** A class without a stereotype shows nothing for it, not even when it is selected: the menu adds it. */
  @Test
  void noPlaceWithoutStereotype() throws ClassDiagramParseException {
    open(BARE);
    selectClass();

    assertTrue(editing.selectedPlaceholders().isEmpty());
    assertFalse(hasStereotypeText());
  }

  /** A class which has a stereotype can't get another one, and its stereotype is a text to change. */
  @Test
  void existingStereotypeIsText() throws ClassDiagramParseException {
    open(WITH_STEREOTYPE);
    selectClass();

    assertFalse(editing.canAddStereotypeTo(editing.selectedClass()));
    assertEquals(stereotypeText(), editing.editableTextAt(centerOf(stereotypeText())));
    assertEquals("Entity", editing.textOf(stereotypeText()));
  }

  /** The row for a new stereotype is in the class, above its name, but not in the diagram until it has a text. */
  @Test
  void newStereotypeRow() throws ClassDiagramParseException {
    open(BARE);
    selectClass();
    double heightBefore = boxArea().getHeight();

    SceneTextArea row = editing.newStereotype(editing.selectedClass());

    assertEquals(row, stereotypeText());
    assertEquals("", editing.textOf(row));
    assertTrue(boxArea().getHeight() > heightBefore);
    assertTrue(row.area.getMaxY() <= boxArea().getY() + boxArea().getHeight() / 2);
    assertNull(classOf(editing.source()).stereotype);
    assertTrue(editing.hasSelection());
  }

  /** A stereotype which is typed into the row is created, marked as added while changes are tracked. */
  @Test
  void createdStereotype() throws ClassDiagramParseException {
    open(BARE);
    selectClass();
    editing.setText(editing.newStereotype(editing.selectedClass()), "Entity");
    assertEquals("Entity", classOf(editing.source()).stereotype);
    assertNull(classOf(editing.source()).stereotypeElementChange);
    assertNull(classOf(editing.source()).stereotypeChange);

    host.trackedChangeSet = "yellow";
    open(BARE);
    selectClass();
    editing.setText(editing.newStereotype(editing.selectedClass()), "Entity");
    assertEquals(ChangeType_V001.ADDED, classOf(editing.source()).stereotypeElementChange.type);
    assertEquals("yellow", classOf(editing.source()).stereotypeElementChange.changeSet);
  }

  /** An empty text or dropping the row leaves the class as it was, and is no operation. */
  @Test
  void droppedStereotypeRow() throws ClassDiagramParseException {
    open(BARE);
    selectClass();
    String before = editing.source();
    editing.setText(editing.newStereotype(editing.selectedClass()), "");
    assertFalse(hasStereotypeText());

    editing.newStereotype(editing.selectedClass());
    editing.discardNewPart();

    assertFalse(hasStereotypeText());
    assertEquals(before, editing.source());
    assertFalse(editing.canUndo());
  }

  /** Whatever else is done while the row is there, the diagram which is written has no blank stereotype. */
  @Test
  void rowIsNeverWritten() throws ClassDiagramParseException {
    open(BARE);
    selectClass();
    editing.newStereotype(editing.selectedClass());

    editing.addClass(300, 60);

    assertNull(classOf(editing.source()).stereotype);
    assertFalse(editing.source().contains("<<"));
  }

  /** A class which a change set has removed can't get a stereotype. */
  @Test
  void removedClassGetsNone() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 60)\n}");
    selectClass();

    assertFalse(editing.canAddStereotypeTo(editing.selectedClass()));
    assertThrows(IllegalArgumentException.class, () -> editing.newStereotype(editing.selectedClass()));
  }

  /** A stereotype which is removed while changes are tracked keeps its text and is struck through; typing it again
   * takes the removal back. */
  @Test
  void removedStereotypeKeepsText() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_STEREOTYPE);
    selectClass();

    editing.setText(stereotypeText(), "");

    ClassModel_V001 removed = classOf(editing.source());
    assertEquals("Entity", removed.stereotype);
    assertEquals(ChangeType_V001.REMOVED, removed.stereotypeElementChange.type);
    assertEquals("", editing.textOf(stereotypeText()));
    assertTrue(editing.canAddStereotypeTo(editing.selectedClass()));

    editing.setText(stereotypeText(), "Entity");

    assertNull(classOf(editing.source()).stereotypeElementChange);
    assertNull(classOf(editing.source()).stereotypeChange);
  }

  /** The menu adds a stereotype to a class whose stereotype is removed by typing into the struck row. */
  @Test
  void removedStereotypeIsTypedInto() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_STEREOTYPE);
    selectClass();
    editing.setText(stereotypeText(), "");

    SceneTextArea row = editing.newStereotype(editing.selectedClass());

    assertEquals(stereotypeText(), row);
    assertEquals("Entity", classOf(editing.source()).stereotype);
  }

  /** A changed stereotype keeps the old text as backup. */
  @Test
  void changedStereotypeTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(WITH_STEREOTYPE);
    selectClass();

    editing.setText(stereotypeText(), "Table");

    assertEquals("Entity", classOf(editing.source()).stereotypeChange.backup);
  }

  /** A stereotype is one line and can't contain the closing angle bracket. */
  @Test
  void invalidStereotypes() throws ClassDiagramParseException {
    open(WITH_STEREOTYPE);

    assertFalse(editing.accepts(stereotypeText(), "a>b"));
    assertFalse(editing.accepts(stereotypeText(), "a\nb"));
    assertTrue(editing.accepts(stereotypeText(), "a b"));
  }
}
