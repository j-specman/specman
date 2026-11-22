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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeEditingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n}";
  private static final String WITH_STEREOTYPE = "classdiagram {\n  class A (11111111) at(10, 60) <<Entity>>\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final AtomicInteger textEdits = new AtomicInteger();
  private DiagramEditing editing;
  private PointerControl control;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host, new EditorTexts(host.locale()));
    control = new PointerControl(editing, () -> { }, text -> textEdits.incrementAndGet(), pixels -> pixels);
  }

  private Rectangle2D boxArea() {
    return editing.scene().elementAreas.get(0).area;
  }

  private void selectClass() {
    editing.selectAt(new Point2D.Double(boxArea().getCenterX(), boxArea().getMaxY() - 2));
  }

  private SceneTextArea placeholder() {
    return editing.scene().placeholderAreas.stream().filter(area -> area.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();
  }

  private SceneTextArea stereotypeText() {
    return editing.scene().textAreas.stream().filter(area -> area.kind == TextKind.STEREOTYPE).findFirst()
      .orElseThrow();
  }

  private static Point2D centerOf(SceneTextArea area) {
    return new Point2D.Double(area.area.getCenterX(), area.area.getCenterY());
  }

  private ClassModel_V001 classOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source).classes().get(0);
  }

  /** A selected class without a stereotype shows a place for it above its top edge, centered, outside of the box. */
  @Test
  void placeAboveSelectedClass() throws ClassDiagramParseException {
    open(BARE);
    assertTrue(editing.selectedPlaceholders().isEmpty());

    selectClass();

    assertEquals(1, editing.selectedPlaceholders().size());
    Rectangle2D place = editing.selectedPlaceholders().get(0);
    assertTrue(place.getMaxY() < boxArea().getY());
    assertEquals(boxArea().getCenterX(), place.getCenterX(), 0.01);
  }

  /** A class which has a stereotype has no place for another one, and its stereotype is a text to change. */
  @Test
  void existingStereotypeIsText() throws ClassDiagramParseException {
    open(WITH_STEREOTYPE);
    selectClass();

    assertTrue(editing.selectedPlaceholders().isEmpty());
    assertEquals(stereotypeText(), editing.editableTextAt(centerOf(stereotypeText())));
    assertEquals("Entity", editing.textOf(stereotypeText()));
  }

  /** A double click on the place of the selected class starts the field, and the class stays selected. */
  @Test
  void doubleClickOnPlace() throws ClassDiagramParseException {
    open(BARE);
    selectClass();

    control.pressed(centerOf(placeholder()));
    control.released(centerOf(placeholder()));
    control.doubleClicked(centerOf(placeholder()));

    assertEquals(1, textEdits.get());
    assertTrue(editing.hasSelection());
  }

  /** A stereotype which is typed into the place is created, marked as added while changes are tracked. */
  @Test
  void createdStereotype() throws ClassDiagramParseException {
    open(BARE);
    selectClass();
    editing.setText(placeholder(), "Entity");
    assertEquals("Entity", classOf(editing.source()).stereotype);
    assertNull(classOf(editing.source()).stereotypeElementChange);

    host.trackedChangeSet = "yellow";
    open(BARE);
    selectClass();
    editing.setText(placeholder(), "Entity");
    assertEquals(ChangeType_V001.ADDED, classOf(editing.source()).stereotypeElementChange.type);
    assertEquals("yellow", classOf(editing.source()).stereotypeElementChange.changeSet);
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
    assertTrue(editing.selectedPlaceholders().isEmpty());

    editing.setText(stereotypeText(), "Entity");

    assertNull(classOf(editing.source()).stereotypeElementChange);
    assertNull(classOf(editing.source()).stereotypeChange);
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
