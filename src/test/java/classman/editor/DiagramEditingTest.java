package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.render.SceneTextArea;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramEditingTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String EMPTY = "classdiagram {\n\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());

  /** The class lands where it was dropped and is reported with ID and name. */
  @Test
  void droppedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(120, 80);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertEquals("NewClass", box.name);
    assertEquals(120, box.position.x);
    assertEquals(80, box.position.y);
    assertTrue(box.id.matches("[0-9a-f]{8}"));
    assertEquals(List.of(editing.source()), host.reportedSources);
  }

  /** Several new classes can be told apart. */
  @Test
  void distinctClasses() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);
    editing.addClass(200, 0);
    editing.addClass(400, 0);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    Set<String> names = new HashSet<>();
    Set<String> ids = new HashSet<>();
    for (ClassModel_V001 box : boxes) {
      names.add(box.name);
      ids.add(box.id);
    }
    assertEquals(3, names.size());
    assertEquals(3, ids.size());
    assertEquals(3, host.reportedSources.size());
  }

  /** The picture follows the diagram after each operation. */
  @Test
  void sceneFollowsSource() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    double widthBefore = editing.scene().width;

    editing.addClass(300, 300);

    assertTrue(editing.scene().width > widthBefore);
  }

  /** While changes are tracked, a new class is marked as added by the change set. */
  @Test
  void trackedClass() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertNotNull(box.change);
    assertEquals(ChangeType_V001.ADDED, box.change.type);
    assertEquals("yellow", box.change.changeSet);
  }

  /** Without tracking, a new class carries no mark. */
  @Test
  void untrackedClass() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(0, 0);

    assertNull(onlyClassOf(editing.source()).change);
  }

  /** What the diagram already has stays when a class is added. */
  @Test
  void existingElementsKept() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.addClass(300, 20);

    List<ClassModel_V001> boxes = modelOf(editing.source()).classes();
    assertEquals(2, boxes.size());
    assertEquals("Book", boxes.get(0).name);
    assertEquals("04de91aa", boxes.get(0).id);
  }

  /** A press inside a box selects it, a press outside deselects. */
  @Test
  void selectingBox() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  class Page (11111111) at(400, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    assertFalse(editing.hasSelection());

    editing.selectAt(new Point2D.Double(15, 25));

    assertTrue(editing.hasSelection());
    assertTrue(editing.selectedArea().contains(15, 25));
    assertFalse(editing.selectedArea().contains(405, 25));

    editing.selectAt(new Point2D.Double(405, 25));

    assertTrue(editing.selectedArea().contains(405, 25));

    editing.selectAt(new Point2D.Double(250, 300));

    assertFalse(editing.hasSelection());
    assertNull(editing.selectedArea());
  }

  /** Where boxes overlap, the one drawn on top is selected. */
  @Test
  void selectingTopmostBox() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Below (04de91aa) at(10, 20)\n  class Above (11111111) at(12, 22)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));

    assertEquals(12, editing.selectedArea().getX(), 0.001);
  }

  /** A class which has just been created is selected. */
  @Test
  void newClassSelected() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.addClass(100, 100);

    assertEquals(100, editing.selectedArea().getX(), 0.001);
    assertEquals(100, editing.selectedArea().getY(), 0.001);
  }

  /** Deselecting leaves nothing selected. */
  @Test
  void deselecting() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    editing.addClass(100, 100);

    editing.deselect();

    assertFalse(editing.hasSelection());
  }

  /** A box which a change set with a special selection color marked gets that color for its selection frame. */
  @Test
  void specialSelectionColor() throws ClassDiagramParseException {
    host.changeColors.selectionColors.put("blue", "#000000");
    String source = "classdiagram {\n  class Book (04de91aa) change(added, blue) at(10, 20)\n"
      + "  class Page (11111111) change(added, yellow) at(400, 20)\n  class Cover (22222222) at(10, 300)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));
    assertEquals(Color.BLACK, editing.specialSelectionColor());

    editing.selectAt(new Point2D.Double(405, 25));
    assertNull(editing.specialSelectionColor());

    editing.selectAt(new Point2D.Double(15, 305));
    assertNull(editing.specialSelectionColor());

    editing.deselect();
    assertNull(editing.specialSelectionColor());
  }

  /** A note can be selected like a class. */
  @Test
  void selectingNote() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(305, 25));

    assertTrue(editing.hasSelection());
    assertEquals(300, editing.selectedArea().getX(), 0.001);
  }

  /** An association class can be selected wherever it stands, also where the layout has placed it. */
  @Test
  void selectingAssociationClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) at(0, 0)\n  class B (bbbbbbbb) at(300, 0)\n"
      + "  association aaaaaaaa -- bbbbbbbb\n  assocclass link\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    Rectangle2D placed = editing.scene().elementAreas.get(2).area;

    editing.selectAt(new Point2D.Double(placed.getCenterX(), placed.getCenterY()));

    assertEquals(placed, editing.selectedArea());
  }

  /** The user's own position of an association class counts. */
  @Test
  void selectingPositionedAssociationClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) at(0, 0)\n  class B (bbbbbbbb) at(300, 0)\n"
      + "  association aaaaaaaa -- bbbbbbbb\n  assocclass link at(100, 200)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(105, 205));

    assertEquals(100, editing.selectedArea().getX(), 0.001);
    assertEquals(200, editing.selectedArea().getY(), 0.001);
  }

  /** Deleting the selected class takes it out of the diagram, and nothing stays selected. */
  @Test
  void deletedClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n  class Page (11111111) at(400, 20)\n"
      + "  association 04de91aa -> 11111111\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.deleteSelected();

    assertEquals(1, modelOf(editing.source()).classes().size());
    assertEquals("Page", modelOf(editing.source()).classes().get(0).name);
    assertTrue(modelOf(editing.source()).relations().isEmpty());
    assertFalse(editing.hasSelection());
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the class stays, marked as removed, but is no longer selected. */
  @Test
  void deletedClassTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.deleteSelected();

    ClassModel_V001 book = onlyClassOf(editing.source());
    assertEquals(ChangeType_V001.REMOVED, book.change.type);
    assertEquals("yellow", book.change.changeSet);
    assertFalse(editing.hasSelection());
  }

  /** Delete with nothing selected does nothing. */
  @Test
  void nothingToDelete() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    editing.deleteSelected();

    assertTrue(host.reportedSources.isEmpty());
  }

  /** In change mode, a class which was added is gone at once. */
  @Test
  void addedClassGoneAtOnce() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);
    editing.addClass(10, 10);

    editing.deleteSelected();

    assertTrue(modelOf(editing.source()).classes().isEmpty());
    assertFalse(editing.hasSelection());
  }

  /** The name area of a class lies in the header of its box, across its width. */
  @Test
  void nameAreaInHeader() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20) { -title : String }\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(15, 25));

    Rectangle2D box = editing.selectedArea();
    Rectangle2D name = textOfKind(editing, TextKind.CLASS_NAME).area;
    assertTrue(box.contains(name));
    assertTrue(name.getHeight() > 0);
    assertTrue(name.getMaxY() < box.getCenterY() + box.getHeight() / 2 - 10);
    assertEquals("Book", editing.textOf(textOfKind(editing, TextKind.CLASS_NAME)));
  }

  /** A note has no name area, and no class is selected. */
  @Test
  void noNameAreaForNote() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);

    editing.selectAt(new Point2D.Double(305, 25));

    assertTrue(editing.hasSelection());
    assertEquals(1, editing.scene().textAreas.size());
    assertEquals(TextKind.NOTE_TEXT, editing.scene().textAreas.get(0).kind);
  }

  /** The new name is reported, once. */
  @Test
  void renamedClass() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.setText(textOfKind(editing, TextKind.CLASS_NAME), "Volume");

    assertEquals("Volume", onlyClassOf(editing.source()).name);
    assertNull(onlyClassOf(editing.source()).nameChange);
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the old name is kept as backup of the change set. */
  @Test
  void renamedClassTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.setText(textOfKind(editing, TextKind.CLASS_NAME), "Volume");

    ClassModel_V001 box = onlyClassOf(editing.source());
    assertEquals("Volume", box.name);
    assertEquals("yellow", box.nameChange.changeSet);
    assertEquals("Book", box.nameChange.backup);
  }

  /** A line break in a name stays in the name, in the source and in the picture. */
  @Test
  void renamedWithLineBreak() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));
    double oneLine = textOfKind(editing, TextKind.CLASS_NAME).area.getHeight();

    editing.setText(textOfKind(editing, TextKind.CLASS_NAME), "Book\nVolume");

    assertEquals("Book\nVolume", onlyClassOf(editing.source()).name);
    assertTrue(textOfKind(editing, TextKind.CLASS_NAME).area.getHeight() > 1.5 * oneLine);
  }

  /** While changes are tracked, a text field gets the pale color of the tracking change set, otherwise none. */
  @Test
  void trackedFieldColor() throws ClassDiagramParseException {
    host.changeColors.strongColors.put("yellow", "#ffff00");
    DiagramEditing editing = new DiagramEditing(EMPTY, host, texts);

    assertNull(editing.trackedFieldColor());

    host.trackedChangeSet = "yellow";

    assertEquals(Color.decode("#ffff99"), editing.trackedFieldColor());
  }

  /** The text of a note can be changed in place, with line breaks. */
  @Test
  void changedNoteText() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(305, 25));

    editing.setText(textOfKind(editing, TextKind.NOTE_TEXT), "First line\nSecond line");

    assertEquals("First line\nSecond line", modelOf(editing.source()).notes().get(0).text);
    assertNull(modelOf(editing.source()).notes().get(0).textChange);
    assertEquals(1, host.reportedSources.size());
  }

  /** While changes are tracked, the old text of a note is the backup of the change set. */
  @Test
  void changedNoteTextTracked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    String source = "classdiagram {\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(305, 25));

    editing.setText(textOfKind(editing, TextKind.NOTE_TEXT), "Forget");

    assertEquals("Forget", modelOf(editing.source()).notes().get(0).text);
    assertEquals("yellow", modelOf(editing.source()).notes().get(0).textChange.changeSet);
    assertEquals("Remember", modelOf(editing.source()).notes().get(0).textChange.backup);
  }

  /** A note text is hit where it is drawn. */
  @Test
  void noteTextHit() throws ClassDiagramParseException {
    String source = "classdiagram {\n  note (22222222) at(300, 20) \"Remember\"\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    Rectangle2D text = textOfKind(editing, TextKind.NOTE_TEXT).area;
    editing.selectAt(new Point2D.Double(text.getCenterX(), text.getCenterY()));

    assertEquals(TextKind.NOTE_TEXT,
      editing.editableTextAt(new Point2D.Double(text.getCenterX(), text.getCenterY())).kind);
    assertNull(editing.editableTextAt(new Point2D.Double(text.getMinX() - 20, text.getMinY() - 20)));
    assertTrue(editing.boxOf(textOfKind(editing, TextKind.NOTE_TEXT)).contains(text));
  }

  /** The same name is no operation. */
  @Test
  void sameNameIsNoStep() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class Book (04de91aa) at(10, 20)\n}";
    DiagramEditing editing = new DiagramEditing(source, host, texts);
    editing.selectAt(new Point2D.Double(15, 25));

    editing.setText(textOfKind(editing, TextKind.CLASS_NAME), "Book");

    assertFalse(editing.canUndo());
  }

  private static final String RELATED = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  association (33333333) 11111111 1 -> * 22222222 \"owns\"\n}";

  /** A relation has areas for its label and both multiplicities, which lie between its two classes. */
  @Test
  void relationTextAreas() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);

    Rectangle2D label = textOfKind(editing, TextKind.RELATION_LABEL).area;
    Rectangle2D from = textOfKind(editing, TextKind.FROM_MULTIPLICITY).area;
    Rectangle2D to = textOfKind(editing, TextKind.TO_MULTIPLICITY).area;

    assertEquals("owns", editing.textOf(textOfKind(editing, TextKind.RELATION_LABEL)));
    assertEquals("1", editing.textOf(textOfKind(editing, TextKind.FROM_MULTIPLICITY)));
    assertEquals("*", editing.textOf(textOfKind(editing, TextKind.TO_MULTIPLICITY)));
    assertTrue(from.getCenterX() < label.getCenterX() && label.getCenterX() < to.getCenterX());
    SceneTextArea fromText = textOfKind(editing, TextKind.FROM_MULTIPLICITY);
    assertTrue(fromText.hitArea().getWidth() > from.getWidth(), "short texts get an area which is easy to hit");
  }

  /** The texts of a relation can be changed without selecting anything, the texts of a class only when it is selected. */
  @Test
  void relationTextEditableWithoutSelection() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);
    Rectangle2D label = textOfKind(editing, TextKind.RELATION_LABEL).area;
    Rectangle2D name = textOfKind(editing, TextKind.CLASS_NAME).area;
    Point2D onLabel = new Point2D.Double(label.getCenterX(), label.getCenterY());
    Point2D onName = new Point2D.Double(name.getCenterX(), name.getCenterY());

    assertFalse(editing.hasSelection());
    assertEquals(TextKind.RELATION_LABEL, editing.editableTextAt(onLabel).kind);
    assertNull(editing.editableTextAt(onName));
    assertEquals(label, editing.boxOf(textOfKind(editing, TextKind.RELATION_LABEL)));
  }

  /** A new label is reported once, an empty one removes it. */
  @Test
  void changedLabel() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);

    editing.setText(textOfKind(editing, TextKind.RELATION_LABEL), "has");
    assertEquals("has", modelOf(editing.source()).relations().get(0).label);
    assertEquals(1, host.reportedSources.size());

    editing.setText(textOfKind(editing, TextKind.RELATION_LABEL), "");
    assertNull(modelOf(editing.source()).relations().get(0).label);
    assertEquals(2, host.reportedSources.size());
  }

  /** A multiplicity at either end is changed, an empty text removes it. */
  @Test
  void changedMultiplicity() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);

    editing.setText(textOfKind(editing, TextKind.FROM_MULTIPLICITY), "0..1");
    editing.setText(textOfKind(editing, TextKind.TO_MULTIPLICITY), "");

    AssociationRelationModel_V001 relation = (AssociationRelationModel_V001) modelOf(editing.source()).relations().get(0);
    assertEquals("0..1", relation.fromMultiplicity);
    assertNull(relation.toMultiplicity);
  }

  /** A multiplicity which isn't a number, a range or a star is not accepted. */
  @Test
  void invalidMultiplicity() throws ClassDiagramParseException {
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);
    SceneTextArea from = textOfKind(editing, TextKind.FROM_MULTIPLICITY);

    for (String valid : List.of("1", "*", "0..1", "1..*", "")) {
      assertTrue(editing.accepts(from, valid), valid);
    }
    for (String invalid : List.of("many", "1..", "a..b", "1 2")) {
      assertFalse(editing.accepts(from, invalid), invalid);
    }
    assertThrows(IllegalArgumentException.class, () -> editing.setText(from, "many"));
    assertFalse(editing.canUndo());
  }

  /** While changes are tracked a label gets the mark, and a multiplicity can be changed but not removed. */
  @Test
  void trackedRelationTexts() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    DiagramEditing editing = new DiagramEditing(RELATED, host, texts);
    SceneTextArea from = textOfKind(editing, TextKind.FROM_MULTIPLICITY);

    editing.setText(textOfKind(editing, TextKind.RELATION_LABEL), "has");
    editing.setText(from, "0..1");

    AbstractRelationModel_V001 relation = modelOf(editing.source()).relations().get(0);
    assertEquals("owns", relation.labelChange.backup);
    assertEquals("1", ((AssociationRelationModel_V001) relation).fromMultiplicityChange.backup);
    assertFalse(editing.accepts(textOfKind(editing, TextKind.FROM_MULTIPLICITY), ""));
  }

  /** A diagram which can't be read can't be edited. */
  @Test
  void unreadableSource() {
    assertThrows(ClassDiagramParseException.class, () -> new DiagramEditing("classdiagram {", host, texts));
  }

  /** A diagram which refers to a class it doesn't have can't be edited. */
  @Test
  void danglingReference() {
    String source = "classdiagram {\n  extends 11111111 -> 22222222\n}";
    assertThrows(IllegalArgumentException.class, () -> new DiagramEditing(source, host, texts));
  }

  private static SceneTextArea textOfKind(DiagramEditing editing, TextKind kind) {
    for (SceneTextArea text : editing.scene().textAreas) {
      if (text.kind == kind) {
        return text;
      }
    }
    throw new AssertionError("no text of kind " + kind);
  }

  private static ClassDiagramModel_V001 modelOf(String source) throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(source);
  }

  private static ClassModel_V001 onlyClassOf(String source) throws ClassDiagramParseException {
    List<ClassModel_V001> boxes = modelOf(source).classes();
    assertEquals(1, boxes.size());
    return boxes.get(0);
  }
}
